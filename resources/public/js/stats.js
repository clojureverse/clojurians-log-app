var ctx = document.getElementById('message-chart');

var messageJson = document.getElementById('message-data').innerHTML;
var messageData = JSON.parse(messageJson.replace(/&quot;/g,'"'));
var labels = messageData.map(function(elem) {
  return elem["day"];
})
var data = messageData.map(function(elem) {
  return elem["msg-count"];
})

var messageChart = new Chart(ctx, {
  type: 'line',
  data: {
      labels: labels,
      datasets: [{
          label: 'No. of slack messages',
          fill: false,
          data: data,
          backgroundColor: 'rgba(255, 99, 132, 0.2)',
          borderColor: 'rgba(54, 162, 235, 1)',
          borderWidth: 2
      }]
  },
});