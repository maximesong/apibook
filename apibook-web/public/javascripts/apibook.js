angular.module('apibookApp', [])
    .controller('APIBookController', ['$scope', function($scope) {
        console.log("HI");
        var postJson = function (url, data, success) {
            $.ajax()
        };
        $scope.search = function () {
            console.log("Search");
            postJson("/api/search")
        };
    }]);

