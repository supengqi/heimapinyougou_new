package com.pinyougou.search.service;

import java.util.List;
import java.util.Map;

public interface ItemSearchService {
	
	
	 /**
	  * 搜索方法
	  * @param searchMap
	  * @return
	  */
	public Map<String, Object> search(Map searchMap);
	
	/**
	 * 导入数据
	 * @param list
	 */
	public void importList(List list);
	
	/**
	 * 删除商品
	 * @param goodsIds
	 */
	public void deleteByGoodsIds(List goodsIds);

}
