function GetToday(day) {
  var today = new Date();
  var dd = today.getDate() + Number(day);
  var mm = today.getMonth() + 1; //January is 0 so need to add 1 to make it 1!
  var yyyy = today.getFullYear();

  if (dd < 10) {
    dd = "0" + dd;
  }

  if (mm < 10) {
    mm = "0" + mm;
  }

  today = yyyy + "-" + mm + "-" + dd;

  return today;
}

export default GetToday;
