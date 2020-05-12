//7806001490 //7705935687
function createRequest(table) {
  $.get(url, {data:"{command:'GETEQUIPMENTS'}"}, function(resp) {
    if(resp.action === 'OK') {
      var searchoption = ":Все объекты";
      for(var i=0;i<resp.data.equipments.length;i++) {
        resp.data.equipments[i].select = false;
        if(searchoption.indexOf(resp.data.equipments[i].group_name) < 0)
          searchoption += ";"+resp.data.equipments[i].group_name+":"+resp.data.equipments[i].group_name;
      }
      
      var dialog = createDialog(requestTable, true, 900, 600, "Создание заявки");
      dialog.dialog('open');
      $('<div>').html("Выберите объекты").appendTo(dialog);
      var equipmentTable = createTable(dialog,'100%','50%',
              [ 
                {
                  label: 'id',
                  name: 'id',
                  key: true,
                  search:true,
                  width:0,
                  hidden:true
                },
                
                {
                  label: 'Объект',
                  name: 'group_name',
                  search:true,
                  stype: "select",
                  searchoptions: { value: searchoption }
                },
                
                {
                  label: 'Заводской номер',
                  name: 'identity_value_name',
                  searchoptions: {
                    clearSearch:false,
                    sopt:['cn']
                  }
                },
                
                {
                  label: 'Адрес',
                  name: 'address',
                  searchoptions: {
                    clearSearch:false,
                    sopt:['cn']
                  }
                },
                
                {
                  label: 'Выбрать', 
                  name: 'select', 
                  dittype: 'checkbox',  editoptions: { value: "True:False" },
                  formatter: 'checkbox', formatoptions:{disabled:false, value: "True:False"}, 
                  align:'center', 
                  search:false}
              ],
              resp.data.equipments,
              1000,
              false).jqGrid("filterToolbar", {searchOperators:false, searchOnEnter: false, ignoreCase: true});
              
      equipmentTable.setGridParam({beforeSelectRow: function (rowid, e) {
          var $self = $(this),
          iCol = $.jgrid.getCellIndex($(e.target).closest("td")[0]),
          cm = $self.jqGrid("getGridParam", "colModel"),
          localData = $self.jqGrid("getLocalRow", rowid);
          if (cm[iCol].name === "select") {
            localData.select = $(e.target).is(":checked");
          }
          return true;
        }}).trigger("reloadGrid");
      
               
      $('<div>').css({'padding-top':70,'padding-bottom':10}).html("Краткое описание проблемы:").appendTo(dialog);
      $('<textarea>').css({resize:'none',width:'100%',height:'25%'}).attr("id","request-text").appendTo(dialog);

      dialog.dialog({
        buttons:{
          "Создать заявку":function() {
            if($('#request-text').val() === '')
              alert("Введите причину заявки");
            else {
              var data = equipmentTable.getGridParam('data');
              var request = {command:'SAVEREQUEST',reason:$('#request-text').val(),equipments:[]};
              for(var i=0;i<data.length;i++)
                if(data[i].select)
                  request.equipments.push(data[i].id);
              $.get(url, {data:JSON.stringify(request)}, function(resp) {
                if(resp.action === 'OK') {
                  reloadRequestTable();
                  dialog.dialog('close');
                }else error(resp);
              });
            }
          }}});
    }else error(resp);
  });
}

$('document').ready(function() {
  refreshComments();
  setInterval(function() {
    refreshComments();
  }, 10000);
  
  $('#requests').click(function () {
    
    var dialog = createDialog($('body'), true, "90%", document.body.clientHeight*0.9, "Заявки");
    dialog.dialog('open');
    
    $('<div>').addClass('request-tool-box').attr("id","request-tool-box").appendTo(dialog);
    
    $('<div>').html("Создать заявку").addClass('creale-request').appendTo('#request-tool-box').click(function() {
      createRequest(requestTable);
    });
    
    requestTable = createTable(dialog, '100%','85%',
            [
              {
                label: 'Заявка №',
                name: 'id',
                width:70,
                searchoptions:{
                  clearSearch:false,
                  sopt:['bw']
                },
                key: true
              },
              
              {
                label: 'Причина',
                name: 'reason',
                searchoptions: {
                  clearSearch:false,
                  sopt:['cn','nc','eq','ne','bw','bn','ew','en']
                },
                cellattr: function (rowId, tv, rawObject, cm, rdata) { return 'style="white-space: normal; padding:10"'; }
              },
              
              {
                label: 'Сооб.',
                align:'center',
                name: 'comments',
                width:40,
                search:false
              },
              
              {
                label: 'Заявлено',
                name: 'startDate', 
                align:'center',
                sorttype: 'date',
                formatter : 'date', 
                formatoptions: {srcformat : 'd.m.y H:i:s', newformat :'d.m.y H:i:s'},
                clearSearch: false,
                searchoptions:{
                  clearSearch:false,
                  sopt:['ge','gt','eq','ne','lt','le'],
                  dataInit : function (elem) {$(elem).datepicker({
                    autoclose: true,
                    onSelect: function () {
                      $(this).keydown();
                    },
                    dateFormat: 'dd.mm.yy'
                  });}
                }
              },
              
              {
                label: 'Принято',
                name: 'acceptDate',
                align:'center',
                sorttype: 'date',
                formatter : 'date', 
                formatoptions: {srcformat : 'd.m.y H:i:s', newformat :'d.m.y H:i:s'},
                clearSearch: false,
                searchoptions:{
                  clearSearch:false,
                  sopt:['ge','gt','eq','ne','lt','le'],
                  dataInit : function (elem) {$(elem).datepicker({
                    autoclose: true,
                    onSelect: function () {
                      $(this).keydown();
                    },
                    dateFormat: 'dd.mm.yy'
                  });}
                }
              },
              
              {
                label: 'В работе',
                name: 'executDate',
                align:'center',
                sorttype: 'date',
                formatter : 'date', 
                formatoptions: {srcformat : 'd.m.y H:i:s', newformat :'d.m.y H:i:s'},
                searchoptions:{
                  clearSearch:false,
                  sopt:['ge','gt','eq','ne','lt','le'],
                  dataInit : function (elem) {$(elem).datepicker({
                    autoclose: true,
                    onSelect: function () {
                      $(this).keydown();
                    },
                    dateFormat: 'dd.mm.yy'
                  });}
                }
              },
              
              {
                label: 'Отклонено',
                name: 'exitDate',
                align:'center',
                sorttype: 'date',
                formatter : 'date', 
                formatoptions: {srcformat : 'd.m.y H:i:s', newformat :'d.m.y H:i:s'},
                searchoptions:{
                  clearSearch:false,
                  sopt:['ge','gt','eq','ne','lt','le'],
                  dataInit : function (elem) {$(elem).datepicker({
                    autoclose: true,
                    onSelect: function () {
                      $(this).keydown();
                    },
                    dateFormat: 'dd.mm.yy'
                  });}
                }
              },
              
              {
                label: 'Исполнено',
                name: 'finishDate',
                align:'center',
                sorttype: 'date',
                formatter : 'date', 
                formatoptions: {srcformat : 'd.m.y H:i:s', newformat :'d.m.y H:i:s'},
                searchoptions:{
                  clearSearch:false,
                  sopt:['ge','gt','eq','ne','lt','le'],
                  dataInit : function (elem) {$(elem).datepicker({
                    autoclose: true,
                    onSelect: function () {
                      $(this).keydown();
                    },
                    dateFormat: 'dd.mm.yy'
                  });}
                }
              }
            ],
            [],
            100,
            true,
            showRequestData)/*.filterToolbar({searchOperators:true, searchOnEnter: false, ignoreCase: true})*/;
    
    reloadRequestTable();
    refreshComments();
  });
});

var requestTable;

function showRequestData(parentRowID, parentRowKey) {
  requestTable.setSelection(parentRowKey,true);
  $.get(url,{data:"{command:'GETREQUESTDATA',id:"+parentRowKey+"}"}, function(resp) {
    if(resp.action === 'OK') {
      $("#" + parentRowID).attr("class","request-block");
      $("#" + parentRowID).mouseover(function(e) {
        requestTable.setSelection(parentRowKey,true);
      });
      
      $('<div>').attr("id","request-box-"+resp.data.id).attr("class","request-box").appendTo("#" + parentRowID);
      
      $('<div>').attr("id","reason-"+resp.data.id).attr("class","reason").appendTo("#" + parentRowID);
      
      $('<div>').attr("class","reason-title").text(resp.data.reason).appendTo("#reason-"+resp.data.id);
      
      $('<div>').attr("class","equipments-title").text("Прикреплённые оъекты").appendTo("#reason-"+resp.data.id);
      var equipmentTable = createTable("#reason-"+resp.data.id,'100%','200',
              [ {label: 'id', name: 'id', key: true, width:0, hidden:true},
                {label: 'Объект', name: 'group_name'},
                {label: 'Заводской номер', name: 'identity_value_name'},
                {label: 'Адрес', name: 'address'}
              ],resp.data.equipments,1000,false);
      
      $('<div>').attr("class","comments-title").text("Комментарии").appendTo("#request-box-" + resp.data.id);
      $('<div>').attr("id","comment-box-"+resp.data.id).attr("class","comment-box").appendTo("#request-box-" + resp.data.id);
      $('<textarea>').attr("id","new-comment-box-"+resp.data.id).attr("class","new-comment-box").appendTo("#request-box-" + resp.data.id).keydown(function(e) {
        if(e.ctrlKey && e.keyCode === 13) {
          $('#comment-button').click();
        }
      });
      $('<button>').attr("class","comment-button").attr("id","comment-button").text("отправить").appendTo("#request-box-" + resp.data.id).click(function() {
        var comment = $("#new-comment-box-"+resp.data.id).val().replace(/\r|\n/g,' ');
        if(comment !== "") {
          $.get(url,{data:"{command:'ADDCOMMENT',id:"+parentRowKey+",text:'"+comment+"'}"}, function(r) {
            if(r.action === 'OK') {
              addComment(r.data);
              $("#new-comment-box-"+resp.data.id).val("");
            }else error(r);
          });
        }
      });
      
      $.each(resp.data.comments, function(i,comment) {
        addComment(comment);
      });
    }else error(resp);
  });
}

function refreshComments() {
  $.get(url,{data:"{command:'GETCOMMENTS'}"}, function(resp) {
    if(resp.action === 'OK') {
      
      var notread = 0;
      var requestComments = {};
      $.each(resp.data.comments, function(i,comment) {
        
        var key = 'id-'+comment.objectId;
        
        if(!requestComments.hasOwnProperty(key))
          requestComments[key] = 0;
        
        requestComments[key] = requestComments[key] + 1;
        
        addComment(comment);
      });
      $('#desktop-url').text("Заявки"+(notread === 0 ? "" : " ("+notread+")"));
      
      console.info(requestComments);
      
      $.each(requestTable.getDataIDs(), function(i,id) {
        if(requestComments.hasOwnProperty('id-'+id))
          requestTable.setRowData(id,{comments:requestComments['id-'+id]});
        else requestTable.setRowData(id,{comments:0});
      });
      
    }else if(resp.data[0] !== -1)
      error(resp);
  });
}

function isMyComment(comment) {
  return comment.author.indexOf("Company") >= 0;
}

function addComment(comment) {
  if($('div').is("#comment-box-"+comment.objectId) && !$('div').is('#comment-'+comment.id)) {
    if(comment.type === 'PROJECT' && !isMyComment(comment)) {
      $.get(url, {data:"{command:'SETWRITABLE', id:"+comment.id+"}"}, function(resp) {
        //if(resp.action === 'OK')
          //refreshComments();
      });
    }
    var prefix = isMyComment(comment) ? "my" : "org";
    $('<div>').addClass(prefix+"-comment-date comment-date").html(new Date(Date.parse(comment.date)).toLocaleString()).appendTo("#comment-box-"+comment.objectId);
    $('<div>').attr("id","comment-"+comment.id).addClass(prefix+"-comment comment").html(comment.text).appendTo("#comment-box-"+comment.objectId);
    $("#comment-box-"+comment.objectId).scrollTop(document.getElementById("comment-box-"+comment.objectId).scrollHeight);
  }
}

function reloadRequestTable() {
  $.get(url, {data:"{command:'SHOWREQUESTS'}"}, function(resp) {
    if(resp.action === 'OK')
      requestTable.setGridParam({data:resp.data.rows}).trigger('reloadGrid');
    else error(resp);
  });
}

function createTable(parent,width,height,columnModel,dataModel,rowNum,subGrid,subGridRowExpanded) {
  var block = $('<div>').css({width:width,height:height}).appendTo(parent);
  var table = $('<table>').attr("id","id-"+new Date().getTime()).appendTo(block).jqGrid({
    datatype: "local",
    data: dataModel,
    altRows:true,
    page: 1,
    colModel: columnModel,
    width:block.width()-5,
    height:block.height()-20,
    rowNum: rowNum,
    scroll: 1, // set the scroll property to 1 to enable paging with scrollbar - virtual loading of records
    emptyrecords: 'Прокрутите таблицу для подкрузки данных', // the message will be displayed at the bottom 
    viewrecords: true,
    subGrid: subGrid, // set the subGrid property to true to show expand buttons for each row
    subGridRowExpanded: subGridRowExpanded // javascript function that will take care of showing the child grid
  });
  
  new ResizeSensor(block, function(){ 
    table.setGridWidth(block.width());
    table.setGridHeight(block.height());
  });
  return table;
}












(function(root, factory) {
  if (typeof define === "function" && define.amd) {
    define(factory);
  } else if (typeof exports === "object") {
    module.exports = factory();
  } else {
    root.ResizeSensor = factory();
  }
}(this, function() {

  // Make sure it does not throw in a SSR (Server Side Rendering) situation
  if (typeof window === "undefined") {
    return null;
  }
  // Only used for the dirty checking, so the event callback count is limited to max 1 call per fps per sensor.
  // In combination with the event based resize sensor this saves cpu time, because the sensor is too fast and
  // would generate too many unnecessary events.
  var requestAnimationFrame = window.requestAnimationFrame ||
    window.mozRequestAnimationFrame ||
    window.webkitRequestAnimationFrame ||
    function(fn) {
      return window.setTimeout(fn, 20);
    };

  /**
   * Iterate over each of the provided element(s).
   *
   * @param {HTMLElement|HTMLElement[]} elements
   * @param {Function}                  callback
   */
  function forEachElement(elements, callback) {
    var elementsType = Object.prototype.toString.call(elements);
    var isCollectionTyped = ('[object Array]' === elementsType ||
      ('[object NodeList]' === elementsType) ||
      ('[object HTMLCollection]' === elementsType) ||
      ('[object Object]' === elementsType) ||
      ('undefined' !== typeof jQuery && elements instanceof jQuery) //jquery
      ||
      ('undefined' !== typeof Elements && elements instanceof Elements) //mootools
    );
    var i = 0,
      j = elements.length;
    if (isCollectionTyped) {
      for (; i < j; i++) {
        callback(elements[i]);
      }
    } else {
      callback(elements);
    }
  }


  var ResizeSensor = function(element, callback) {
    function EventQueue() {
      var q = [];
      this.add = function(ev) {
        q.push(ev);
      };

      var i, j;
      this.call = function() {
        for (i = 0, j = q.length; i < j; i++) {
          q[i].call();
        }
      };

      this.remove = function(ev) {
        var newQueue = [];
        for (i = 0, j = q.length; i < j; i++) {
          if (q[i] !== ev) newQueue.push(q[i]);
        }
        q = newQueue;
      };

      this.length = function() {
        return q.length;
      };
    }

    /**
     *
     * @param {HTMLElement} element
     * @param {Function}    resized
     */
    function attachResizeEvent(element, resized) {
      if (!element) return;
      if (element.resizedAttached) {
        element.resizedAttached.add(resized);
        return;
      }

      element.resizedAttached = new EventQueue();
      element.resizedAttached.add(resized);

      element.resizeSensor = document.createElement('div');
      element.resizeSensor.className = 'resize-sensor';
      var style = 'position: absolute; left: 0; top: 0; right: 0; bottom: 0; overflow: hidden; z-index: -1; visibility: hidden;';
      var styleChild = 'position: absolute; left: 0; top: 0; transition: 0s;';

      element.resizeSensor.style.cssText = style;
      element.resizeSensor.innerHTML =
        '<div class="resize-sensor-expand" style="' + style + '">' +
        '<div style="' + styleChild + '"></div>' +
        '</div>' +
        '<div class="resize-sensor-shrink" style="' + style + '">' +
        '<div style="' + styleChild + ' width: 200%; height: 200%"></div>' +
        '</div>';
      element.appendChild(element.resizeSensor);

      if (element.resizeSensor.offsetParent !== element) {
        element.style.position = 'relative';
      }

      var expand = element.resizeSensor.childNodes[0];
      var expandChild = expand.childNodes[0];
      var shrink = element.resizeSensor.childNodes[1];
      var dirty, rafId, newWidth, newHeight;
      var lastWidth = element.offsetWidth;
      var lastHeight = element.offsetHeight;

      var reset = function() {
        expandChild.style.width = '100000px';
        expandChild.style.height = '100000px';

        expand.scrollLeft = 100000;
        expand.scrollTop = 100000;

        shrink.scrollLeft = 100000;
        shrink.scrollTop = 100000;
      };

      reset();

      var onResized = function() {
        rafId = 0;

        if (!dirty) return;

        lastWidth = newWidth;
        lastHeight = newHeight;

        if (element.resizedAttached) {
          element.resizedAttached.call();
        }
      };

      var onScroll = function() {
        newWidth = element.offsetWidth;
        newHeight = element.offsetHeight;
        dirty = newWidth !== lastWidth || newHeight !== lastHeight;

        if (dirty && !rafId) {
          rafId = requestAnimationFrame(onResized);
        }

        reset();
      };

      var addEvent = function(el, name, cb) {
        if (el.attachEvent) {
          el.attachEvent('on' + name, cb);
        } else {
          el.addEventListener(name, cb);
        }
      };

      addEvent(expand, 'scroll', onScroll);
      addEvent(shrink, 'scroll', onScroll);
    }

    forEachElement(element, function(elem) {
      attachResizeEvent(elem, callback);
    });

    this.detach = function(ev) {
      ResizeSensor.detach(element, ev);
    };
  };

  ResizeSensor.detach = function(element, ev) {
    forEachElement(element, function(elem) {
      if (!elem) return;
      if (elem.resizedAttached && typeof ev === "function") {
        elem.resizedAttached.remove(ev);
        if (elem.resizedAttached.length()) return;
      }
      if (elem.resizeSensor) {
        if (elem.contains(elem.resizeSensor)) {
          elem.removeChild(elem.resizeSensor);
        }
        delete elem.resizeSensor;
        delete elem.resizedAttached;
      }
    });
  };

  return ResizeSensor;
}));