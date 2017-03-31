package com.neeq.crawler.consumer.companyInfo.bean;

/**
 * Created by bj on 16/7/26.
 */
public class TopTenHolders {
    String data;
    String limitedQuantity;//限制股数
    String last_quantity; //持股数量
    String quantity;//持股数
    String num;//排名
    String name;//姓名
    String change_quantity;//改变数量
    String unlimited_quantity;////非限制股数
    String ratio;//持股比例

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getLimitedQuantity() {
        return limitedQuantity;
    }

    public void setLimitedQuantity(String limitedQuantity) {
        this.limitedQuantity = limitedQuantity;
    }

    public String getLast_quantity() {
        return last_quantity;
    }

    public void setLast_quantity(String last_quantity) {
        this.last_quantity = last_quantity;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChange_quantity() {
        return change_quantity;
    }

    public void setChange_quantity(String change_quantity) {
        this.change_quantity = change_quantity;
    }

    public String getUnlimited_quantity() {
        return unlimited_quantity;
    }

    public void setUnlimited_quantity(String unlimited_quantity) {
        this.unlimited_quantity = unlimited_quantity;
    }

    public String getRatio() {
        return ratio;
    }

    public void setRatio(String ratio) {
        this.ratio = ratio;
    }
}
