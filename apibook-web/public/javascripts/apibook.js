angular.module('apibookApp', [])
    .controller('APIBookController', ['$scope', function($scope) {
        console.log("HI");
        $scope.search = function() {
            console.log("Search");
        }
    }]);

