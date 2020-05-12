Date.prototype.daysInMonth = function() {
  return 33 - new Date(this.getFullYear(), this.getMonth(), 33).getDate();
};

Date.prototype.format = function() {
  return this.getDate()+"."+(this.getMonth()+1)+"."+this.getFullYear();
};

var Client = function(opts) {
  
  this.url            = opts !== undefined && 'url'            in opts ? opts.url            : "lk";
  this.loginBlock     = opts !== undefined && 'loginBlock'     in opts ? opts.loginBlock     : "login-block";
  this.page           = opts !== undefined && 'page'           in opts ? opts.page           : "page";
  this.documentsBlock = opts !== undefined && 'documentsBlock' in opts ? opts.documentsBlock : "documents-block";
  this.name           = opts !== undefined && 'name'           in opts ? opts.name           : "";
  this.timeoutOpacity = opts !== undefined && 'timeoutOpacity' in opts ? opts.timeoutOpacity : 200;
  this.dates          = opts !== undefined && 'dates'         in opts ? opts.dates           : {};
  this.sort           = opts !== undefined && 'sort'          in opts ? opts.sort            : {Дата:"DESC"};
  
  this.previewDocument = function(id) {
    window.open(this.url+"?action=preview&id="+id,"_blank");
  };
  
  this.login = function(response, func) {
    var c = this;
    c.name = response['client-name'];
    $("#"+c.loginBlock).animate({"opacity":"0"}, c.timeoutOpacity, function() {
      $("#"+c.loginBlock).hide();
      $("#"+c.page).animate({"opacity":"1"}, c.timeoutOpacity, function() {
        if(func !== undefined)
          func.call();
      });
    });
  };
  
  this.error = function(response) {
    var c = this;
    var msg = "";
    $.each(response['body'], function(i) {
      if(response['body'][i] === -1) {
        c.logout();
      }else msg += response['body'][i]+"</br>";
    });
    if(msg !== "")
      window.alert(msg);
  };
  
  this.validateInn = function(inn) {
    return /^\d{10,12}$/.test($("#inn").val()) && !/^\d{11}$/.test(inn);
  };
  
  this.sortDocuments = function(column) {
    if(column in this.sort) {
      if(this.sort[column] === "DESC")
        delete this.sort[column];
      else this.sort[column] = "DESC";
    }else this.sort[column] = "ASC";
  };
  
  this.logout = function(func) {
    var c = this;
    $("#"+c.page).animate({"opacity":"0"}, c.timeoutOpacity, function() {
      $("#"+c.loginBlock).show(0, function() {
        $("#"+c.loginBlock).animate({"opacity":"1"}, c.timeoutOpacity, function() {
          if(func !== undefined)
            func.call();
        });
      });
    });
  };
  
  this.loadDocuments = function(finish,error) {
    var c = this;
    $("#"+c.documentsBlock).animate({opacity:0}, c.timeoutOpacity, function() {
      $.get(c.url, {action:"get-documents", dates:c.dates, sort:c.sort}, function(response) {
        if(response['action'] === "OK") {
          $("#"+c.documentsBlock).html("");
          var table = $("<table/>", {class:'document-table'}).appendTo("#"+c.documentsBlock);
          var tr    = $("<tr/>", {id:"header"}).appendTo(table).append("<th>№</th>");

          //Заголовок
          $.each(response['body'][0], function(key,val) {
            if(key !== "id") {
              var sortType = key in c.sort && c.sort[key] === "ASC" ? "^" : key in c.sort && c.sort[key] === "DESC" ? "v" : "&nbsp;";
              $("<th/>", {
                click: function() {
                  /*var div = $("<div/>").appendTo(tr).css({
                    "position":"absolute", 
                    "top":c.mouseY+"px", 
                    "left":c.mouseX+"px", 
                    "background-color":"red", 
                    "width":"300px", 
                    "height":"300px"});*/
                  c.sortDocuments(key);
                  c.loadDocuments();
                }
              }).appendTo(tr).append(key+"<span style='float:right;'>"+sortType+"</span>");
            }
          });

          $.each(response['body'], function(i) {
            var tr = $("<tr/>", {
              click: function(){
                c.previewDocument(response['body'][i]['id']);
              }
            }).appendTo(table).append("<td>"+(i+1)+"</td>");
            if(i%2 === 0)
              tr.attr("class","color-tr");
            $.each(response['body'][i], function(key,val) {
              if(key !== "id")
                tr.append("<td>"+val+"</td>");
            });
          });
          $("#"+c.documentsBlock).animate({opacity:1}, c.timeoutOpacity, function() {
            if(finish !== undefined)
            finish.call();
          });
        }else {
          c.error(response);
          if(error !== undefined)
            error.call();
        }
      });
    });
  };
};