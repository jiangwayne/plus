package com.plus.server.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.plus.server.common.PageDefault;
import com.plus.server.common.SysConfig;
import com.plus.server.dal.CommentDAO;
import com.plus.server.dal.OrderDAO;
import com.plus.server.dal.ProductDAO;
import com.plus.server.dal.ProductSpecDAO;
import com.plus.server.dal.TransactionDAO;
import com.plus.server.model.Order;
import com.plus.server.model.Product;
import com.plus.server.model.ProductSpec;
import com.plus.server.model.Transaction;

@Service
public class OrderService {
	private static final Logger log = LoggerFactory.getLogger(OrderService.class);
	@Autowired
	private OrderDAO orderDAO;
	@Autowired
	private ProductSpecDAO productSpecDAO;
	@Autowired
	private ProductDAO productDAO;
	@Autowired
	private CommentDAO commentDAO;
	@Autowired
	private TransactionDAO transactionDAO;

	/**
	 * 创建订单
	 * 
	 * @param userId
	 * @param productSpecId
	 * @param count
	 */
	@Transactional(rollbackFor = Exception.class)
	public Order createOrder(Long userId, Long productSpecId, Integer count, String boardingIds) throws Exception {
		log.info("创建订单，userId={},productSpecId={},count={}", userId, productSpecId, count);
		if (userId == null || productSpecId == null || count == null) {
			throw new Exception("参数不能为空");
		}
		if(count > SysConfig.product_max_buy_count){
			throw new Exception("超过最大购买数["+SysConfig.product_max_buy_count+"]");
		}
		ProductSpec ps = productSpecDAO.selectByPrimaryKeyForUpdate(productSpecId);
		if (ps == null) {
			throw new Exception("产品规格错误");
		}
		if(ps.getCountMax() < ps.getCountSale()+count){
			throw new Exception("剩余库存不足");
		}
		Order orderParam = new Order();
		orderParam.setValid(1);
		orderParam.setUserId(userId);
		orderParam.setStatus(20);
		List<Order> existNoPayOrderList = this.orderDAO.selectByModel(orderParam);
		if(existNoPayOrderList != null && existNoPayOrderList.size() >= SysConfig.max_no_pay_order_count){
			throw new Exception("您已有["+SysConfig.max_no_pay_order_count+"]笔订单等待付款");
		}
		Product pro = productDAO.selectByPrimaryKey(ps.getProductId());
		Date now = new Date();
		Order order = new Order();
		order.setCount(count);
		order.setGmtCreate(now);
		order.setPriceEach(ps.getPriceCurrent());
		order.setPriceTotal(count * ps.getPriceCurrent());
		order.setProductId(ps.getProductId());
		order.setProductSpecId(productSpecId);
		order.setBoardingIds(boardingIds);
		if (pro.getPayType() == 1) {// 支付类型（1-直接支付，2-不直接支付（生成的是待确认订单））
			order.setStatus(20);// 状态（10-待确认，20-待付款，30-待评价，40-已评价，50-已取消）
		} else if (pro.getPayType() == 2) {
			order.setStatus(10);
		} else {
			throw new Exception("产品支付类型错误");
		}
		order.setUserId(userId);
		order.setValid(1);
		this.orderDAO.insert(order);
		Order updateOrder = new Order();
		updateOrder.setId(order.getId());
		SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
		updateOrder.setOrderNo("NO"+f.format(now)+order.getId());
		this.orderDAO.updateByPrimaryKeySelective(updateOrder);
		// 修改 产品规格的库存
		ProductSpec proSpecParam = new ProductSpec();
		proSpecParam.setId(productSpecId);
		proSpecParam.setCountSale(count);
		productSpecDAO.updateCountSaleByPrimaryKey(proSpecParam);
		// 修改 产品的销量
		Product proParam = new Product();
		proParam.setId(pro.getId());
		proParam.setSaleCount(proParam.getSaleCount()==null?count:(proParam.getSaleCount()+ count));
		productDAO.updateCommentCountSaleCountByPrimaryKey(proParam);
		
		return orderDAO.selectByPrimaryKey(order.getId());
	}

	/**
	 * 查询订单
	 *
	 * @return
	 */
	public PageInfo<Order> selectByModel(Order order, int page, int pageSize) {
		log.info("订单查询，order={},page={},pageSize={}", JSON.toJSONString(order), page, pageSize);

		if (page <= 0) {
			page = PageDefault.PAGE_NUM_DEFAULT;
		}
		if (pageSize <= 0) {
			pageSize = PageDefault.PAGE_SIZE_DEFAULT;
		}
		PageHelper.startPage(page, pageSize);
		List<Order> orderList = orderDAO.selectByModel(order);
		PageInfo<Order> pageInfo = new PageInfo<Order>(orderList);
		return pageInfo;
	}

	public PageInfo<Order> selectByProductType(Order order, int page, int pageSize, int productType, String keywords) {
		log.info("订单查询，order={},page={},pageSize={}", JSON.toJSONString(order), page, pageSize);
		if (page <= 0) {
			page = PageDefault.PAGE_NUM_DEFAULT;
		}
		if (pageSize <= 0) {
			pageSize = PageDefault.PAGE_SIZE_DEFAULT;
		}
		PageHelper.startPage(page, pageSize);
		List<Order> orderList = orderDAO.selectByModel(order);
		PageInfo<Order> pageInfo = new PageInfo<Order>(orderList);
		return pageInfo;
	}

	public Order selectById(Long id) {
		return this.orderDAO.selectByPrimaryKey(id);
	}

	/**
	 * 支付
	 * @param userId
	 * @param orderId
	 * @param money
	 * @param type
	 * @param partnerOrderNo
	 */
	@Transactional(rollbackFor = Exception.class)
	public void pay(Long userId, Long orderId, Integer money, Integer type,
			String partnerOrderNo,String ip) throws Exception{
		if(userId == null || orderId == null || money == null || type == null){
			throw new Exception("失败");
		}
		Order order = this.orderDAO.selectByPrimaryKey(orderId);
		if(order == null || order.getUserId() != userId || order.getPriceTotal() != money.intValue()){
			throw new Exception("失败");
		}
		Date now = new Date();
		Transaction tr = new Transaction();
		
		tr.setGmtCreate(now);
		tr.setIp(ip);
		tr.setPartnerOrderNo(partnerOrderNo);
		tr.setPartnerType(type.toString());
		tr.setTradeNo(orderId.toString());
		tr.setTransAmount(money);
		tr.setTransStatus(20);//交易状态：10-初始，20-成功，30-失败
		tr.setUserId(userId.toString());
		tr.setValid(1);
		transactionDAO.insert(tr);
		
		Order updateOrder = new Order();
		updateOrder.setId(orderId);
		SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
		updateOrder.setGmtModify(now);
		updateOrder.setStatus(30);//状态（10-待确认，20-待付款，30-待评价，40-已评价，50-已取消）
		this.orderDAO.updateByPrimaryKeySelective(updateOrder);
	}

}
