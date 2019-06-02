$(document).ready(function () {
  $('[data-toggle="offcanvas"]').click(function () {
    $('.row-offcanvas').toggleClass('active')
  });
});

Ext.onReady(function(){
  var ip = "";
  var cname = "";

  if(window.returnCitySN){
    ip = returnCitySN.cip;
    cname = returnCitySN.cname;
  }
  
  BASE_URL = "/";

  var LoginName = document;
  var Password = document;

  getMessage(LoginName.value, Password.value, ip, cname);
});

function getMessage(loginName, password, ip, ipFrom) {
    var r = {
        url : BASE_URL + "",
        method : "GET",
        params : {
            account: loginName,
            password: password,
            ip : ip,
            ipFrom: ipFrom
        },
        callback : function (options, success, response) {
            if(success){
                showMessage(response.responseText);
            }
        }
    };

    Ext.Ajax.request(r);
    function showInfo(info) {

    }
}



