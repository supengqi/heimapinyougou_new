app.controller('searchController',function($scope,$location,searchService){
	// 定义搜索对象
	$scope.searchMap ={'keywords':'','category':'','brand':'','spec':{},'price':'','pageNo':1,'pageSize':40,'sort':'','sortField':''}; // 搜索对象
	
	// 加载查询字符串
	$scope.loadkeywords = function() {
		$scope.searchMap.keywords = $location.search()['keywords'];
		$scope.search(); // 查询
	}
	
	// 搜索
	$scope.resultMap ={};
	$scope.search = function() {
		//  转换为数字
		$scope.searchMap.pageNo= parseInt($scope.searchMap.pageNo);
		
		searchService.search($scope.searchMap).success(
			function(response) {
				$scope.resultMap = response;
				buildPageLabel(); // 查询后调用
			}
		);
	}
	
	// 添加搜索项 改变searchMap的值
	$scope.addSearchItem = function(key,value) {
		 
		if (key == 'category' || key == 'brand' || key == 'price') { // 如果用户点击的是分类或品牌
			$scope.searchMap[key]=value;
			
		} else {  // 用户点击的是规格
			$scope.searchMap.spec[key] = value;
		}
		$scope.search();//执行搜索
	}
	
	//移除复合搜索条件
	$scope.removeSearchItem = function(key){
		if (key == "category" || key == "brand" || key == 'price') {//如果是分类或品牌
			$scope.searchMap[key] = "";
		} else {//否则是规格
			delete $scope.searchMap.spec[key];//移除此属性
		}
		$scope.search();//执行搜索
		
	}
	
	// 构建分页标签(totalPages为总页数)
	buildPageLabel = function() {
		$scope.pageLabel = [];//新增分页栏属性
		var maxPageNo = $scope.resultMap.totalPages;//得到最后页码
		var firstPage = 1;//开始页码
		var lastPage = maxPageNo;//截止页码
		$scope.firstDot = true;//前面有点
		$scope.lastDot = true;//后边有点
		
		if($scope.resultMap.totalPages > 5) { //如果总页数大于 5 页,显示部分页码
			if($scope.searchMap.pageNo <= 3){//如果当前页小于等于 3
				lastPage = 5; //前 5 页
				$scope.firstDot = false;//前面无点
			} else if ($scope.searchMap.pageNo >= lastPage - 2 ) {//如果当前页大于等于最大页码-2
				firstPage = maxPageNo-4; //后 5 页
				$scope.lastDot = false;//后边无点
			} else { //显示当前页为中心的 5 页
				firstPage = $scope.searchMap.pageNo - 2;
				lastPage = $scope.searchMap.pageNo + 2;
				$scope.firstDot = true;//前面有点
				$scope.lastDot = true;//后边有点
			}
		} else {
			$scope.firstDot=false;//前面无点
			$scope.lastDot=false;//后边无点
		}
		// 循环产生页码标签
		for (var i=firstPage;i<=lastPage;i++) {
			$scope.pageLabel.push(i);
		}
	}
	
	//分页查询
	$scope.queryByPage=function(pageNo){
		// 验证
		if (pageNo < 1 || pageNo > $scope.resultMap.totalPages){
			return;
		}		
		$scope.searchMap.pageNo = pageNo;
		$scope.search();//查询
	}
	
	// 判断当前页是否是第一页
	$scope.isTop=function() {
		if ($scope.searchMap.pageNo == 1) {
			return true;
		} else {
			return false;
		}
	}
	
	// 判断当前页是否是最后一页
	$scope.isLast=function() {
		if ($scope.searchMap.pageNo == $scope.resultMap.totalPages) {
			return true;
		} else {
			return false;
		}
	}
	
	// 判断是否是当前页 加蓝色高亮  active
	$scope.isCurrentPage = function() {
		if ($scope.resultMap.pageNo == $scope.searchMap.pageNo) {
			return true;
		} else {
			return false;
		}
	}
	
	// 排序查询
	$scope.sortSearch = function(sortField, sort) {
		$scope.searchMap.sortField = sortField;
		$scope.searchMap.sort = sort;
		
		$scope.search();//查询
	}
	
	// 判断关键字是不是品牌列表
	$scope.keywordsIsBrand = function() {
		for (var i = 0; i < $scope.resultMap.brandList.length; i++) {
			if ($scope.searchMap.keywords.indexOf($scope.resultMap.brandList[i].text) >= 0) {  // indexOf没有包含返回-1
				// 如果包含
				return true;
			}
		}
		return false;
	}
	
});