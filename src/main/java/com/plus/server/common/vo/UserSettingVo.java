package com.plus.server.common.vo;

/**
 * Created by jiangwulin on 16/6/1.
 */
public class UserSettingVo {
    /** 语言（1:中文,2:英文） */
    private Integer languageType;

    /** 时间区（1~24） */
    private Integer timezone;

    /** 货币（1:CNY,2:USD,3:BGP,4:EUR,5:HKD） */
    private Integer currency;

    /** 出行日期类型（1:灵活,2:固定） */
    private Integer travelDateType;

    /** 充许经停（0:,1:,2:） */
    private Integer allowStop;

    public Integer getLanguageType() {
        return languageType;
    }

    public void setLanguageType(Integer languageType) {
        this.languageType = languageType;
    }

    public Integer getTimezone() {
        return timezone;
    }

    public void setTimezone(Integer timezone) {
        this.timezone = timezone;
    }

    public Integer getCurrency() {
        return currency;
    }

    public void setCurrency(Integer currency) {
        this.currency = currency;
    }

    public Integer getTravelDateType() {
        return travelDateType;
    }

    public void setTravelDateType(Integer travelDateType) {
        this.travelDateType = travelDateType;
    }

    public Integer getAllowStop() {
        return allowStop;
    }

    public void setAllowStop(Integer allowStop) {
        this.allowStop = allowStop;
    }

}
