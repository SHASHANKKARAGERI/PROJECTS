<?php
session_start();
require 'db.php';

if(isset($_GET['pid']))
{
    $pid = $_GET['pid'];

    $sql = "SELECT * FROM fproduct WHERE pid='$pid'";
    $result = mysqli_query($conn,$sql);
    $row = mysqli_fetch_assoc($result);
}

if(isset($_POST['update']))
{
    $pid = $_POST['pid'];
    $price = $_POST['price'];

    $sql = "UPDATE fproduct SET price='$price' WHERE pid='$pid'";

    if(mysqli_query($conn,$sql))
    {
        echo "<script>
                alert('Price Updated Successfully');
                window.location='productMenu.php';
              </script>";
    }
}
?>

<!DOCTYPE html>
<html>
<head>
    <title>Edit Product Price</title>
</head>
<body>

<center>
<h2>Edit Product Price</h2>

<form method="POST">
    <input type="hidden" name="pid"
           value="<?php echo $row['pid']; ?>">

    <h3><?php echo $row['product']; ?></h3>

    Current Price:
    <input type="text"
           name="price"
           value="<?php echo $row['price']; ?>"
           required>

    <br><br>

    <input type="submit"
           name="update"
           value="Update Price">
</form>

</center>

</body>
</html>