package com.nowcoder.community.entity;



/**
 * Page类封装的是分页所需要的信息和方法
 */
public class Page {
    //当前页数
    private int current=1;
    //每页显示多少条帖子
    private int limit = 10;
    //总共有多少条帖子
    private int rows;
    //访问服务器的该页的访问路径
    private String path;

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        if (current >= 1) this.current = current;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        if (limit >= 1 && limit <= 100) this.limit = limit;

    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        if (rows >= 0) this.rows = rows;

    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * 该方法是根据当前页数，来获取分页的起始行，从而完成sql的分页查询
     * @return
     */
    public int getOffset() {
        return (current - 1) * limit;
    }

    /**
     * 该方法是获取总页数
     * @return
     */
    public int getTotalPages() {
        return (rows + limit - 1) / limit;
    }

    /**
     * 该方法是根据当前页来获取页面显示的起始页,如果当前页-2小于起始页，那么就起始页固定为1
     * @return
     */
    public int getFrom() {
        int start = current - 2;
        return start < 1 ? 1 : start;
    }

    /**
     * 该方法根据当前页确定页面显示页码的末尾页，如果当前页+2大于了总页数，那么末尾页就是总页数
     * @return
     */
    public int getTo() {
        int to = current + 2;
        int total = getTotalPages();
        return to > total ? total : to;
    }
}
