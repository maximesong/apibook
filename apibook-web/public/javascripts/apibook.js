angular.module('apibookApp', [])
    .controller('APIBookController', ['$scope', '$http', function($scope, $http) {
        console.log("HI");
        var postJson = function (url, data, success, error) {
            $.ajax({
                method: "POST",
                contentType: "application/json",
                url: url,
                data: data,
                success: success,
                error: error,
                dataType: "json"
            });
        };
        $scope.search = function () {
            var searchOptions = {
                searchText: $scope.searchText
            };
            var response = $http.post("/api/search", searchOptions)
            response.success(function(data) {
                console.log(data);
                $scope.items = data.result;
                console.log($scope.items);
            })
            /*
            console.log("Search");
            postJson("/api/search", JSON.stringify(searchOptions), function (data) {
                console.log(data);
                $scope.items = data.result;
                $digest();
            });
            */
        };
    }]);

