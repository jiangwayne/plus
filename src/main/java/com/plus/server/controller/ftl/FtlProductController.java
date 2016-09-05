package com.plus.server.controller.ftl;

import java.text.SimpleDateFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.github.pagehelper.PageInfo;
import com.plus.server.common.util.BeanMapper;
import com.plus.server.common.vo.ProductVo;
import com.plus.server.common.vo.resp.FtlCommonResp;
import com.plus.server.common.vo.resp.ProductResp;
import com.plus.server.controller.BaseController;
import com.plus.server.controller.LoginController;
import com.plus.server.model.Airport;
import com.plus.server.model.City;
import com.plus.server.model.Country;
import com.plus.server.model.Product;
import com.plus.server.service.AreaService;
import com.plus.server.service.ProductService;

@Controller
@RequestMapping(value = "ftl/product")
public class FtlProductController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private ProductService productService;
    @Autowired
    private AreaService areaService;

    @RequestMapping(value = "/list")
    public ModelAndView list(Model model, Integer type,Integer page, Integer pageSize) {
    	System.out.println("ffff");
    	model.addAttribute("type", type);
		Product pro = new Product();
		pro.setType(type);
		pro.setValid(1);
		try {
			PageInfo<Product> pageInfo = productService.selectByModel(pro, page, pageSize);
			if(pageInfo != null && pageInfo.getList() != null){
				List<ProductVo> productList = BeanMapper.mapList(pageInfo.getList(), ProductVo.class);
				for(ProductVo vo : productList){
					fillDateStr(vo);
				}
				model.addAttribute("list", productList);
			}
			model.addAttribute("pages",pageInfo.getPages());
			model.addAttribute("page",pageInfo.getPageNum());
			model.addAttribute("total",pageInfo.getTotal());
		} catch (Exception e) {
			log.error("", e);
		}
		
		return new ModelAndView("productList.ftl");
    }
    private void fillDateStr(ProductVo vo){
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd Hh:mm:ss");
		if(vo.getGmtCreate() != null){
			vo.setGmtCreateStr(f.format(vo.getGmtCreate()));
		}
		if(vo.getGmtModify() != null){
			vo.setGmtModifyStr(f.format(vo.getGmtModify()));
		}
	}
    @RequestMapping(value = "/queryById")
	public ModelAndView queryById(Model model, Long productId) {
		log.info("产品查询---productId={}", productId);
		model.addAttribute("productId",productId);
		ProductResp r = new ProductResp();
		if(productId != null){
			Product product = productService.selectById(productId);
			if(product != null){
				ProductVo vo = BeanMapper.copy(product, new ProductVo());
				fillDateStr(vo);
				model.addAttribute("s",product);
			}
		}
		return new ModelAndView("productInfo.ftl");
	}
    
	@RequestMapping(value = "/loadCountry")
	@ResponseBody
	public FtlCommonResp loadCountry() {
		FtlCommonResp r = new FtlCommonResp();
		Country c = new Country();
		c.setValid(1);
		List<Country> list = areaService.selectCountryByModel(c);
		r.setSuccess(true);
		r.setData(list);
		return r;
	}
	@RequestMapping(value = "/loadCity")
	@ResponseBody
	public FtlCommonResp loadCity(Integer countryId) {
		FtlCommonResp r = new FtlCommonResp();
		City c = new City();
		c.setValid(1);
		c.setCountryId(countryId);
		List<City> list = areaService.selectCityByModel(c);
		r.setSuccess(true);
		r.setData(list);
		return r;
	}
	@RequestMapping(value = "/loadAirport")
	@ResponseBody
	public FtlCommonResp loadAirport(Integer cityId) {
		FtlCommonResp r = new FtlCommonResp();
		Airport c = new Airport();
		c.setValid(1);
		c.setCityId(cityId);
		List<Airport> list = areaService.selectAirportByModel(c);
		r.setSuccess(true);
		r.setData(list);
		return r;
	}
    
}
