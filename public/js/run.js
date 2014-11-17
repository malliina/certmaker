var webSocket;

var tableContent;
var resultContent;
var preContent;

var onconnect = function (payload) {
    var id = $("#plan").html();
    webSocket.send(JSON.stringify({cmd: "uuid", uuid: id}));
    setFeedback("Connected.");
};
var onmessage = function (payload) {
    var event = jQuery.parseJSON(payload.data);
    switch (event.event) {
        case "line":
            append(event.line);
            break;
        case "result":
            $("#result").show();
            showAlert("#successFeedback", "Done.");
            break;
        case "error":
            showAlert("#errorFeedback", event.error);
            break;
    }
};
var showAlert = function (id, text) {
    var elem = $(id);
    elem.html(text);
    elem.show();
};
var append = function (line) {
    //tableContent.append("<tr><td>" + line + "</td></tr>");
    preContent.append(line + "\n");
};
var onclose = function (payload) {
    setFeedback("Connection closed.");
};
var onerror = function (payload) {
    setFeedback("Connection error.");
};
var setFeedback = function (fb) {
    $('#status').html(fb);
};
$(document).ready(function () {
    tableContent = $("#logTableBody");
    resultContent = $("#resultTableBody");
    preContent = $("#output");
});