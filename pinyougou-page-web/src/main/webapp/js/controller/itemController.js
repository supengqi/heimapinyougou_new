app.controller('itemController',function($scope){
	
	$scope.specificationItems={}; // 存储用户选择的规格
	
	// 购物车数量加减
	$scope.addNum=function(x) {
		$scope.num+=x;
		if($scope.num < 1) {
			$scope.num = 1;
		}
	}
	
	// 用户选择规格
	$scope.selectSpecification=function(name,value) {
		$scope.specificationItems[name]=value;
		searchSku(); // 查询SKU
	}
	
	// 是否被选中
	$scope.isSelected=function(name,value) {
		if($scope.specificationItems[name]==value) {
			return true;
		} else {
			return false;
		}
		$scope.sku={id:0,title:'------',price:0}  // 没有匹配显示  
		// 没有匹配规格的应该不能选择 跟京东淘宝一样
	}
	
	$scope.sku={}; // 当前选择的SKU
	
	// 加载默认SKU
	$scope.loadSku=function() {
		$scope.sku=skuList[0];
		$scope.specificationItems = JSON.parse(JSON.stringify($scope.sku.spec)) ; // 赋值深克隆
	}
	
	// 匹配两个对象值是否相等   规格变化
	matchObject=function(map1,map2) {
		for(var k in map1) {
			if(map1[k] != map2[k]) {
				return false;
			}
		}
		
		for(var k in map2) {
			if(map2[k] != map1[k]) {
				return false;
			}
		}
		// 不是包含关系，一个不等就false
		return true;
	}
	
	// 根据规格查找SKU
	searchSku=function() {
		for(var i = 0; i <= skuList.length; i++) {
			if(matchObject(skuList[i].spec, $scope.specificationItems)) {
				// 相等
				$scope.sku = skuList[i];
				return;
			}
		
		}
	}
	
	// 添加商品到购物车
	$scope.addToCart=function() {
		alert('SKUID:'+$scope.sku.id);
	}
});