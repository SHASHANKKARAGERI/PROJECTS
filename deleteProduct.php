<?php
session_start();
require 'db.php';

if(isset($_GET['pid']))
{
    $pid = $_GET['pid'];

    $sql = "DELETE FROM fproduct WHERE pid='$pid'";

    if(mysqli_query($conn, $sql))
    {
        echo "<script>
                alert('Product Removed Successfully');
                window.location='productMenu.php';
              </script>";
    }
    else
    {
        echo "Error deleting product";
    }
}
?>