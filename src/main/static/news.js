/*
 *  Abby Parker
 *  CSIS 612
 *
 *  On page load, initialize the word map and the top 20 and bottom 20 lists
*/
$(document).ready(function () {
    var dataString = $('#wordMapData').text();
    try {
        var data = $.parseJSON(dataString);
        initializeWordMap(data);
        populateTop20List(data);
        populateBottom20List(data);
    } catch (e) {
        $('#result-alert-error').show();
    }
});

function initializeWordMap(data) {
    if (data.length > 180) {
        data = data.slice(0, 180);
    }

    Highcharts.chart('wordMapContainer', {
        series: [{
            type: 'wordcloud',
            data: data,
            name: 'Occurrences'
        }],
        title: {
            text: 'Top Trending Words in the US'
        }
    });
}

function populateTop20List(data) {
    var header = "<h1>Top 20 Words</h1>"
    var list = "<ol>"
    for (i = 0; i < 20 && i < data.length; i++) {
        list = list + "<li>" + data[i].name + "</li>"
    }
    list = list + "</ol>";
    var updatedHtml = header + list;
    $('#top20').html(updatedHtml)
}

function populateBottom20List(data) {
    var header = "<h1>20 Least Common Words</h1>"
    var list = "<ol>"
    for (i = data.length-1; i >= 0 && i >= (data.length -20); i--) {
        list = list + "<li>" + data[i].name + "</li>"
    }
    list = list + "</ol>";
    var updatedHtml = header + list;
    $('#bottom20').html(updatedHtml)
}