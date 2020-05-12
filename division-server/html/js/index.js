var url = 'pc';
    
  Date.prototype.daysInMonth = function() {
    return 33 - new Date(this.getFullYear(), this.getMonth(), 33).getDate();
  };

  Date.prototype.format = function() {
    return this.getDate()+"."+(this.getMonth()+1)+"."+this.getFullYear();
  };

  var kvart = {
    4:['ДЕКАБРЬ' ,'НОЯБРЬ' ,'ОКТЯБРЬ'],
    3:['СЕНТЯБРЬ','АВГУСТ' ,'ИЮЛЬ'   ],
    2:['ИЮНЬ'    ,'МАЙ'    ,'АПРЕЛЬ' ],
    1:['МАРТ'    ,'ФЕВРАЛЬ','ЯНВАРЬ' ]
  };

  var command = {
    SINEUP          :'\'SINEUP\'',
    GETCONTRACTS    :'\'GETCONTRACTS\'',
    GETYEARS        :'\'GETYEARS\'',
    SHOWDOCS        :'\'SHOWDOCS\'',
    PREVIEWDOCUMENT :'\'PREVIEWDOCUMENT\'',
    PREVIEWACT      :'\'PREVIEWACT\'',
    DOWNLOADDOCUMENT:'\'DOWNLOADDOCUMENT\'',
    DOWNLOADZIP     :'\'DOWNLOADZIP\'',
    GETPASSWORD     :'\'GETPASSWORD\'',
    LOGOUT          :'\'LOGOUT\''
  };

  var now = new Date();
  var currentYear    = now.getFullYear();
  var currentMonth   = now.getMonth();
  var currentKvartal = currentMonth <= 2 ? 1 : currentMonth <= 5 ? 2 : currentMonth <= 7 ? 3 : 4;

  var option = {
    period   : [{
        year     : now.getFullYear(),
        month    : now.getMonth(),
        kvartal  : now.getMonth() <= 2 ? 1 : now.getMonth() <= 5 ? 2 : now.getMonth() <= 7 ? 3 : 4
    }],
    year     : now.getFullYear(),
    month    : now.getMonth(),
    kvartal  : now.getMonth() <= 2 ? 1 : now.getMonth() <= 5 ? 2 : now.getMonth() <= 7 ? 3 : 4,
    contract : []
  };

  function indexPeriod(y, kv, m) {
    var i = 0;
    for(i=0;i<option.period.length;i++) {
      var p = option.period[i];
      if(p.year === y && p.kvartal === kv && p.month === m)
        return i;
    }
    return -1;
  }

  function removePeriod(y, kv, m) {
    var index = indexPeriod(y, kv, m);
    if(index >= 0) {
      option.period.splice(index, 1);
    }
  }

  function putPeriod(y, kv, m) {
    var index = indexPeriod(y, kv, m);
    if(index < 0) {
      index = option.period.length;
      option.period.push({
        year:y,
        kvartal:kv,
        month:m
      });
    }
    return option.period[index];
  }

  function error(r) {
    var err = 'Внимание!';
    if(r.data[0] === -1) {
      sineup();
    }else {
      $.each(r.data, function(i) {
        err += '\n'+r.data[i];
      });
      window.alert(err);
    }
  }
  
  function sineup() {
    $('.client-block').animate({'opacity':'0'}, 200);
    $('.document-block').animate({'opacity':'0'}, 200);
    $('body').remove('#sineup-block');
    $('#sineup-block').remove('*');
    var sineupBlock  = $('<div>').css({'position':'absolute'}).appendTo('body').attr('id','sineup-block').css('opacity','0');
    var infoBlock    = $('<div>').attr('id','info-block').appendTo(sineupBlock).html("Если Вы не знаете или забыли пароль от кабинета,<br/>\n\
введите свой ИНН и жмите на ссылку<br/><b>\"Получить пароль клиента\"</b>.<br/>Система отправит Вам электронное письмо с<br/>параметрами входа на адрес указанный при<br/>заключении договора.");
    var table        = $('<table>').appendTo(sineupBlock)
            .append("<tr><td>ИНН:</td><td><input name='inn' id='inn-text'/></td></tr>")
            .append("<tr><td>Пароль:</td><td><input name='password' id='password-text' type='password'/></td></tr>")
            .append("<tr><td colspan='2' id='sineup-td'></td></tr>");
    $('<span>').attr('id','get-password').html('Получить пароль клиента').appendTo('#sineup-td').click(function() {
      var inn = $('#inn-text').val();
      if(validateInn(inn) === false) {
        $('#info-block').css({'color':'red','opacity':'0'}).html('Не'+(inn === '' ? '' : 'верно')+' введён ИНН').animate({'opacity':'1'}, 500);
      }else {
        $.get(url, {data:'{command:'+command.GETPASSWORD+',inn:\''+$('#inn-text').val()+'\'}'}, function(resp) {
          if(resp.action === 'OK') {
            $('#info-block').css({'color':'gray','opacity':'0'}).html(resp.data.message).animate({'opacity':'1'}, 500);
          }else $('#info-block').css({'color':'red','opacity':'0'}).html(resp.data.join('<br/>')).animate({'opacity':'1'}, 500);
        });
      }
    });
    $('#sineup-td').append("<input type='button' id='sineup' value='вход'/>");
    sineupBlock.css({
      'position':'absolute',
      'left':'50%',
      'top':'50%',
      'margin-left':'-'+(sineupBlock.width()/2),
      'margin-top':'-'+(sineupBlock.height()/2)
    });
    infoBlock.height(infoBlock.height());
    infoBlock.width(infoBlock.width());
    $('#inn-text').focus().keypress(function(e) {
      if(e.keyCode === 13) {
        $('#password-text').focus();
      }
    });
    $('#password-text').keypress(function(e) {32
      if(e.keyCode === 13) {
        $('#sineup').click();
      }
    });
    sineupBlock.animate({'opacity':'1'},300);
    $('#sineup').click(function() {
      $('#info-block').animate({'opacity':'0'}, 200, function() {
        var err = [];
        var inn = $('#inn-text').val();
        var password = $('#password-text').val();
        if(validateInn(inn) === false) {
          err[err.length] = '<b>Не'+(inn === '' ? '' : 'верно')+' введён ИНН.</b> (ИНН может содержать только цифры и состоять 10 или 12 цифр.)';
        }
        if(password === '') {
          err[err.length] = '<b>Не введён пароль.</b> (Если Вы не знаете или забыли пароль от кабинета, введите свой ИНН и жмите на ссылку <b>\"Получить пароль клиента\"</b>)';
        }
        if(err.length > 0) {
          $('#info-block').css('color','red').html(err.join('<br/>')).animate({'opacity':'1'}, 200);
        }else {
          $.get(url, {data:'{command:'+command.SINEUP+',inn:\''+inn+'\',password:\''+password+'\'}'}, function(resp) {
            if(resp.action === 'OK') {
              $('#sineup-block').animate({'opacity':'0'},300,function() {
                initContracts();
              });
              
            }else {
              alert(resp.data[1]);
              $('#info-block').css('color','red').html(resp.data[0]).animate({'opacity':'1'}, 200);
            }
          });
        }
      });
    });
  }

  function validateInn(inn) {
    return /^\d{10,12}$/.test(inn) && !/^\d{11}$/.test(inn);
  }

  function initContracts() {
    $.get(url, {data:'{command:'+command.GETCONTRACTS+'}'}, function(resp) {
      if(resp.action === 'OK') {
        $('#main-tool-bar').animate({'opacity':'1'},1000);
        $('#header').show().animate({'opacity':'0'}, 200);
        var clientBlock = $('<div/>').addClass('client-block').appendTo('body').css('opacity','0');
        var clientName  = $('<div/>').html(resp.data.name).addClass('client-name').appendTo(clientBlock);
        $.each(resp.data.contracts, function(i,elem) {
          option.contract[option.contract.length] = elem.id;
          var contract = $('<div>').addClass('contract').appendTo(clientBlock);
          var number   = $('<div>').addClass('contract-number').on('click', {id:elem.id}, function(e) {
            if(option.contract.indexOf(e.data.id) >= 0) {
              option.contract.splice(option.contract.indexOf(e.data.id),1);
              $('#img-'+elem.id).animate({'opacity':'0.3'}, 200);
            }else {
              option.contract[option.contract.length] = e.data.id;
              $('#img-'+elem.id).animate({'opacity':'1'}, 200);
            }

            loadDocuments(true, option.period);
          }).appendTo(contract);

          var table      = $('<table>').appendTo(number);
          var tr         = $('<tr>').appendTo(table);
          var img_td     = $('<td>').appendTo(tr);
          var img        = $('<img>').attr('id','img-'+elem.id).css('opacity','1').attr('src','img/true.png').appendTo(img_td);
          var name_td    = $('<td>').css('width','100%').appendTo(tr).html('Договор № '+elem.number+'<br/><span style=\'font-size:10pt;\'>'+elem.name+'</span>');
          var balance_td = $('<td>').appendTo(tr);

          if(elem.balance !== 0) {
            balance_td.addClass('contract-balance-'+(elem.balance < 0 ? 'minus' : 'plus')).html(elem.balance);
          }
        });
        $('#header').animate({'opacity':'1'}, 2000);
        clientBlock.animate({'opacity':'1'}, 300, function() {
          initDocuments();
        });
      }else error(resp);
    });
  }

  function initDocuments() {
    $.get(url, {data:'{command:'+command.GETYEARS+'}'}, function(resp) {
      if(resp.action === 'OK') {

        var min_year = Number(resp['data']['min']);
        var max_year = Number(resp['data']['max']);
        ///////////////
        var document_block = $('<div/>')
                .addClass('document-block')
                .appendTo("body").css('opacity','0');
        ///////////////
        //Цикл по годам
        while(max_year >= min_year) {
          /////////////
          var year_block = $('<div/>')
                  .addClass('year-block')
                  .appendTo(document_block);
          /////////////
          var year = $('<span/>')
                  .addClass('year')
                  .html(max_year+' год.')
                  .on('click',{year:max_year}, function(e) {
                    var kvartals = $('#'+e.data.year+'-kvartals');
                    if(kvartals.css('display') === 'none') {
                      kvartals.slideDown("slow");
                    }else {
                      var actions_kvartals = [];
                      var actions_month = [];
                      for(var kv=1;kv<=4;kv++) {
                        actions_kvartals.push($('#'+e.data.year+'-'+kv).slideUp('slow'));
                        var months = kv === 1 ? [0,1,2] : kv === 2 ? [3,4,5] : kv === 3 ? [6,7,8] : [9,10,11];
                        for(var m=0;m<months.length;m++) {
                          actions_month.push($('#'+e.data.year+'-'+kv+'-'+months[m]).slideUp('slow'));
                          removePeriod(e.data.year, kv, months[m]);
                        }
                      }
                      $.when.apply($,actions_month).done(function() {
                        $.when.apply($,actions_kvartals).done(function() {
                          kvartals.slideUp('slow');
                        });
                      });

                      //kvartals.slideUp("slow");
                    }
                  }).appendTo(year_block);
          /////////////
          $('<span/>').appendTo(year_block).html('&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;');
          var i = 1;
          //Цикл по кварталам
          for(i=1;i<=4;i++) {
            //////////////////
            var kv = $('<span/>')
                    .addClass('kv')
                    .html(i+'кв').on('click', {year:max_year,kv:i}, function(e) {
                      var kvartals = $('#'+e.data.year+'-kvartals');
                      var kvartal  = $('#'+e.data.year+'-'+e.data.kv);
                      if(kvartal.css('display') === 'none') {
                        kvartals.slideDown("slow", function() {
                          kvartal.slideDown("slow");
                        });
                      }else {

                        var months = e.data.kv === 1 ? [0,1,2] : e.data.kv === 2 ? [3,4,5] : e.data.kv === 3 ? [6,7,8] : [9,10,11];
                        var actions = [];
                        for(var m=0;m<months.length;m++) {
                          actions.push($('#'+e.data.year+'-'+e.data.kv+'-'+months[m]).slideUp('slow'));
                          removePeriod(e.data.year, e.data.kv, months[m]);
                        }
                        $.when.apply($,actions).done(function() {
                          kvartal.slideUp('slow');
                        });
                      }
                    }).appendTo(year_block);
            ////////////////////
          }
          $('<span/>').appendTo(year_block).html('&nbsp;');
          if(max_year >= 2015) {
            var actyear = $('<span/>')
                      .addClass('act')
                      .html("(акт сверки за "+max_year+" год.)")
                      .on('click', {year:max_year}, function(e) {
                        openDialog(document, "Акт сверки за "+max_year+" год.", url+'?data={command:'+command.PREVIEWACT+',contract:['+option.contract.join(',')+'],year:'+e.data.year+'}');
                        //window.open(url+'?data={command:'+command.PREVIEWACT+',contract:['+option.contract.join(',')+'],year:'+e.data.year+'}', '_blank');
                      })
                      .appendTo(year_block);
            }

          /////////////////////
          var kvartals = $('<div/>')
                  .css({display: 'none'})
                  .attr('id',max_year+'-kvartals')
                  .appendTo(year_block);
          ////////////////////

          i = 1;
          //Цикл по кварталам
          for(i=4;i>=1;i--) {
            ///////////////////////
            var kvartal_block = $('<div/>')
                    .addClass('kvartal-block')
                    .on('click', {year:max_year,kv:i}, function(e) {

                    }).appendTo(kvartals);
            ///////////////////////
            var kvartal = $('<span/>')
                    .addClass('kvartal')
                    .html(i+' квартал '+max_year+'г. ')
                    .on('click', {year:max_year,kv:i}, function(e) {
                      var kvartal  = $('#'+e.data.year+'-'+e.data.kv);
                      if(kvartal.css('display') === 'none') {
                        kvartal.slideDown('slow');
                      }else {
                        var months = e.data.kv === 1 ? [0,1,2] : e.data.kv === 2 ? [3,4,5] : e.data.kv === 3 ? [6,7,8] : [9,10,11];
                        var actions = [];
                        for(var m=0;m<months.length;m++) {
                          actions.push($('#'+e.data.year+'-'+e.data.kv+'-'+months[m]).slideUp('slow'));
                          removePeriod(e.data.year, e.data.kv, months[m]);
                        }
                        $.when.apply($,actions).done(function() {
                          kvartal.slideUp('slow');
                        });
                      }
                    }).appendTo(kvartal_block);
            var act = $('<span/>')
                    .addClass('act')
                    .html("(акт сверки)")
                    .on('click', {year:max_year,kv:i}, function(e) {
                      openDialog(document, "Акт сверки", url+'?data={command:'+command.PREVIEWACT+',contract:['+option.contract.join(',')+'],year:'+e.data.year+',kv:'+e.data.kv+'}');
                      //window.open(url+'?data={command:'+command.PREVIEWACT+',contract:['+option.contract.join(',')+'],year:'+e.data.year+',kv:'+e.data.kv+'}', '_blank');
                    })
                    .appendTo(kvartal_block);
            ///////////////////////
            //////////////////////
            var months = $('<div/>')
                    .css({display: 'none'})
                    .attr('id',max_year+'-'+i)
                    .appendTo(kvartal_block);
            ///////////////////////
            $.each(kvart[i], function(m) {
              /////////////////////
              var month_block = $('<div/>')
                      .addClass('month-block')
                      .appendTo(months);
              /////////////////////
              var monthIndex = i*3-(m+1);
              /////////////////////
              var month = $('<span/>')
                      .addClass('month')
                      .html(kvart[i][m])
                      .on('click', {year:max_year, kvartal:i, monthIndex:monthIndex}, function(e) {
                        var documents = $('#'+e.data.year+'-'+e.data.kvartal+'-'+e.data.monthIndex);
                        if(documents.css('display') === 'none') {
                          loadDocuments(false, [putPeriod(e.data.year, e.data.kvartal, e.data.monthIndex)]);
                        }else {
                          documents.slideUp('slow');
                          removePeriod(e.data.year, e.data.kvartal, e.data.monthIndex);
                        }
                      }).appendTo(month_block);
              /////////////////////
              /////////////////////
              var documents = $('<div/>')
                     .css({display:'none'})
                      .attr('id',max_year+'-'+i+'-'+monthIndex)
                      .appendTo(month_block);
            });
          }
          max_year--;
        }
        document_block.animate({'opacity':'1'}, function() {
          loadDocuments(false, option.period);
        });
      }else error(resp);
    });
  }

  var douwnloadDocument = [];

  var periodsToLoad = [];

  function getDocuments() {
    if(periodsToLoad.length > 0) {
      var period = periodsToLoad.pop();
      $.get(url, {data:'{command:'+command.SHOWDOCS+', contract:['+option.contract.join(',')+'], year:'+period.year+', month:'+period.month+'}'}, 
      function(resp) {
        var documents = $('#'+period.year+'-'+period.kvartal+'-'+period.month);
        documents.empty();
        var doc_table = $('<table/>')
          .attr('id','documents-'+period.year+'-'+period.kvartal+'-'+period.month)
          .css('opacity','0')
          .addClass('document-table')
          .appendTo(documents);

        if(resp.action === 'OK') {
          if(resp.data.length === 0) {
            $('<tr><td>документы отсутствуют</td></tr>').appendTo(doc_table);
          }else {
            $.each(resp.data, function(i) {
              var tr = $('<tr/>')
                      .attr('id','tr-'+resp.data[i].id)
                      .appendTo(doc_table);

              if(resp.data[i].templ === 0) {
                tr.css('cursor','auto');
              }

              if(i%2 === 0) {
                tr.addClass('document-color');
              }

              $('<td/>')
                      .css({width:'20px'})
                      .attr('id','download-td-'+resp.data[i].id)
                      .on('click', function() {
                        if(resp.data[i].templ > 0) {
                          checkDocument(resp.data[i].id);
                        }
                      })
                      .appendTo(tr);

              if(resp.data[i].templ > 0) {
                $('<img/>').css({'opacity':douwnloadDocument.indexOf(resp.data[i].id) >= 0 ? '1' : '0.3'}).attr('id','download-img-'+resp.data[i].id).attr('src','img/true.png').appendTo('#download-td-'+resp.data[i].id);
              }

              $('<td/>')
                      .attr('id','td-name-'+resp.data[i].id)
                      .html(resp.data[i].name)
                      .on('click', function() {
                        if(resp.data[i].templ > 0) {
                          checkDocument(resp.data[i].id);
                        }
                      })
                      .appendTo(tr);

              $('<td/>')
                      .attr('id','td-date-'+resp.data[i].id)
                      .html(resp.data[i].date)
                      .on('click', function(){
                        if(resp.data[i].templ > 0) {
                          checkDocument(resp.data[i].id);
                        }
                      })
                      .appendTo(tr);

              $('<td/>')
                      .html(resp.data[i].templ > 0 ? '<img src=img/preview.gif>' : '')
                      .on('click', {id:resp.data[i].id}, function(e) {
                        if(resp.data[i].templ > 0) {
                          openDialog(document, resp.data[i].name+" №"+resp.data[i].number, url+'?data={command:'+command.PREVIEWDOCUMENT+', id:'+resp.data[i].id+'}');
                          //window.open(url+'?data={command:'+command.PREVIEWDOCUMENT+', id:'+resp.data[i].id+'}', '_blank');
                        }
              }).appendTo(tr);

              $('<td/>')
                      .html(resp.data[i].templ > 0 ? '<a href="'+url+'?data={command:'+command.DOWNLOADDOCUMENT+', id:'+resp.data[i].id+'}"><img src=img/download.jpg></a>' : '')
                      .appendTo(tr);
            });
          }

          $('#'+period.year+'-kvartals').slideDown('slow', function() {
            $('#'+period.year+'-'+period.kvartal).slideDown('slow',function() {
              documents.slideDown('slow', function() {
                doc_table.animate({'opacity':'1'},100);
              });
            });
          });
        }else {
          doc_table.remove();
          error(resp);
        }
      }).always(function() {
        getDocuments();
      });
    }
  }


  function loadDocuments(reload, periods) {
    if(periodsToLoad.length === 0) {
      for(i=0;i<periods.length;i++) {
        periodsToLoad.push(periods[i]);
      }
      if(reload === true) {
        //reloadDocuments(0);
        var documents_str = [];
        var doc_table_str = [];

        for(i=0;i<periods.length;i++) {
          var period = periodsToLoad[i];
          documents_str.push('#'+period.year+'-'+period.kvartal+'-'+period.month);
          doc_table_str.push('#documents-'+period.year+'-'+period.kvartal+'-'+period.month);
        }

        $.when(
                $(doc_table_str.join(',')).animate({'opacity':'0'}, 200, function() {
                  $(documents_str.join(',')).slideUp('slow');
                })).then(function() {
                  getDocuments();
                });
      }else {
        getDocuments();
      }
    }
  }

  function reloadDocuments(index) {
    if(index < periodsToLoad.length) {
      var period = periodsToLoad[index];
      var documents = $('#'+period.year+'-'+period.kvartal+'-'+period.month);
      var doc_table = $('#documents-'+period.year+'-'+period.kvartal+'-'+period.month);
      doc_table.animate({'opacity':'0'}, 100, function() {
        documents.slideUp('slow', function() {
          reloadDocuments(++index);
        });
      });
    }else {
      getDocuments();
    }
  }

  function checkDocument(id) {
    var td = $('#download-td-'+id);
    var img = $('#download-img-'+id);
    var addDocument = douwnloadDocument.indexOf(id) < 0;

    if(addDocument === true) {
      img.animate({'opacity':'1'}, 500);
      douwnloadDocument[douwnloadDocument.length] = id;
    }else {
      img.animate({'opacity':'0.3'}, 500);
      douwnloadDocument.splice(douwnloadDocument.indexOf(id), 1);
    }

    var zip = $('#documents-to-download');

    if(zip.length === 0) {
      zip = $('<div/>')
              .attr('id','documents-to-download')
              .appendTo('body');

      var download_zip = $('<div/>')
              .attr('id','download-zip')
              .appendTo(zip);
    }

    $('#download-zip').html('<a href="'+url+'?data={command:'+command.DOWNLOADZIP+',ids:['+douwnloadDocument.join(',')+']}">Скачать одним архивом</a>&nbsp;&nbsp;');

    $('<div>').attr('id','cancel-download-zip').appendTo('#download-zip').html('X').click(function() {
      var i = 0;
      for(i=douwnloadDocument.length-1;i>=0;i--) {
        checkDocument(douwnloadDocument[i]);
      }
    });

    if(douwnloadDocument.length === 0) {
      $('#zip-'+id).animate({'opacity':'0'}, 100).slideUp('slow', function() {
        $('#zip-'+id).remove();
        zip.animate({'opacity':'0'}, 200);
      });
      //zip.animate({'opacity':'0'}, 200, function(){$('#zip-'+id).remove();});
    }else {
      if(addDocument === true) {
        $('<div/>')
                .addClass('zip-doc')
                .attr('id','zip-'+id)
                .css({'display':'none', 'opacity':'0'})
                .html($('#td-name-'+id).html()+' от '+$('#td-date-'+id).html())
                .prependTo(zip).slideDown('slow').animate({'opacity':'1'}, 100);

        $('#download-zip').prependTo(zip);

        var height = 0;
        $.each(zip.children(), function(i,e) {
          height += $(e).height();
        });

        if(height >= 100) {
          zip.css({
            'height':zip.height()+'px',
            'overflow':'auto'
          });
        }
      }else {
        $('#zip-'+id)
                .animate({'opacity':'0'}, 100).slideUp('slow', function() {
                    $('#zip-'+id).remove();
                  });
        var height = 0;
        $.each(zip.children(), function(i,e) {
          height += $(e).height();
        });

        if(height < 100) {
          zip.css('height','');
          zip.css('overflow','');
        }
      }
      zip.animate({'opacity':'1'}, 200);
    }
  }

  $(document).ready(function() {
    var ua = detect.parse(navigator.userAgent);
    if(ua.browser.family === 'IE' && Number(ua.browser.version) < 9) {
      $('<div>').html('Ваш браузер не сможет корректно отображать страницу. \n\
Обновите свой Internet Explorer до версии 9 и выше или воспользуйтесь другим браузером \n\
(Firefox, Google Chrome, Opera и т.д.).<br/><br/>Приносим свои извинения.').appendTo('body').css({
      'width':'600px',
      'height':'400px',
      'position':'absolute',
      'left':'50%',
      'top':'50%',
      'margin-left':'-300px',
      'margin-top':'-200px',
      'font-size':'20pt',
      'font-weight':'bold',
      'color':'gray'
    });
    }else {
      $.ajaxSetup({cache: false}); 

      $('#logout').click(function() {
        $.get(url, {data:'{command:'+command.LOGOUT+'}'}, function(resp) {
          if(resp.action === 'OK') {
            window.location.reload();
          }
        });
      });

      initContracts();
    }
  });

  function openDialog(parent, title,url) {
    var div = $('<div/>').css({
      width:"95%",
      height:"95%",
      position:"absolute",
      opacity:0.5,
      "background-image":"url('/img/loading.gif')",
      "background-repeat": "no-repeat",
      "background-position":"center center"
    });
    var frame = $('<iframe>')
            .css({width:"100%",height:"100%"})
            .attr("id","id-frame").attr("class","document-frame")
            .on("load", function() {
              div.animate({opacity:0},500,function() {
                div.css({"z-index":-1});
              });
            });
    var dialog = createDialog(parent, true, "90%", document.body.clientHeight-document.body.clientHeight*0.1, title).append(div).append(frame);
    
    dialog.dialog({close: function() {
      frame.attr("src","#");
      dialog.remove();
      $('body').remove(dialog);
    }});
    
    dialog.dialog('open');
    frame.attr("src",url);
  }
  
  function createDialog(parent, modul, width, height, title) {
    var dialog = $('<div>').appendTo('body').dialog({
      autoOpen: false,
      modal: modul,
      width: width,
      height: height,
      title: title,
      classes: {
          "ui-dialog": "ui-dialog",
          "ui-dialog-titlebar": "ui-dialog-titlebar"
      },
      position: { my: "center", at: "center", of: parent },
      show: {
        effect: "fade",
        duration: 500
      },
      hide: {
        effect: "fade",
        duration: 500
      },
      close: function() {
        dialog.remove();
        $('body').remove(dialog);
      },
      open: function() {
      }
    });
    return dialog;
  }