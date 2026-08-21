<?php
	session_start();
	require 'db.php';
    if(!isset($_SESSION['logged_in']) OR $_SESSION['logged_in'] == 0)
	{
		$_SESSION['message'] = "You need to first login to access this page !!!";
		header("Location: Login/error.php");
	}
    $bid = $_SESSION['id'];
    if(isset($_GET['flag']))
    {
        $pid = $_GET['pid'];

        $sql = "INSERT INTO Mycart (bid,pid)
               VALUES ('$bid', '$pid')";
        $result = mysqli_query($conn, $sql);
    }


	if(isset($_GET['remove']))
{
    $product_id = $_GET['remove'];

    $delete_query = "DELETE FROM mycart WHERE pid='$product_id'";
    mysqli_query($conn, $delete_query);

    echo "<script>alert('Product Removed From Cart')</script>";
    echo "<script>window.open('myCart.php','_self')</script>";
}

?>
<!DOCTYPE html>
<html lang="en">
	<head>
		<meta charset="UTF-8">
		<title>AgroCulture: My Cart</title>
		<meta http-equiv="content-type" content="text/html; charset=utf-8" />
		<meta name="description" content="" />
		<meta name="keywords" content="" />
		<link href="bootstrap\css\bootstrap.min.css" rel="stylesheet">
		<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
        <script src="bootstrap\js\bootstrap.min.js"></script>
		<!--[if lte IE 8]><script src="css/ie/html5shiv.js"></script><![endif]-->
		<link rel="stylesheet" href="login.css"/>
		<script src="js/jquery.min.js"></script>
		<script src="js/skel.min.js"></script>
		<script src="js/skel-layers.min.js"></script>
		<script src="js/init.js"></script>
		<noscript>
			<link rel="stylesheet" href="css/skel.css" />
			<link rel="stylesheet" href="css/style.css" />
			<link rel="stylesheet" href="css/style-xlarge.css" />
		</noscript>
		<!--[if lte IE 8]><link rel="stylesheet" href="css/ie/v8.css" /><![endif]-->
	</head>
	<body class>

		<?php
			require 'menu.php';
			function dataFilter($data)
			{
				$data = trim($data);
				$data = stripslashes($data);
				$data = htmlspecialchars($data);
				return $data;
			}
		?>

		<!-- One -->
			<section id="main" class="wrapper style1 align-center" >
				<div class="container">
						<h2>My Cart</h2>

				<section id="two" class="wrapper style2 align-center">
				<div class="container">
					<div class="row">
					<?php
                        $sql = "SELECT * FROM mycart WHERE bid = '$bid'";
                        $result = mysqli_query($conn, $sql);
						while($row = $result->fetch_array()):
                            $pid = $row['pid'];
                            $sql = "SELECT * FROM fproduct WHERE pid = '$pid'";
                            $result1 = mysqli_query($conn, $sql);
                            $row1 = mysqli_fetch_assoc($result1);
							$picDestination = "images/productImages/".$row1['pimage'];
						?>
							<div class="col-md-4">
							<section>
							<strong><h2 class="title" style="color:black; "><?php echo $row1['product'].'';?></h2></strong>
							<a href="review.php?pid=<?php echo $row1['pid'] ;?>" > <img class="image fit" src="<?php echo $picDestination;?>" alt=""  /></a>

							<div style="align: left">
							<blockquote>
                            <?php echo "Type : ".$row1['pcat']; ?><br>
							<?php echo "Price : ".$row1['price']; ?> /-<br><br>

                            <a href="myCart.php?remove=<?php echo $pid; ?>">
                            <button style="background:red;color:white;border:none;padding:8px 15px;border-radius:5px;cursor:pointer;">
                             Delete
                             </button>
                             </a>

                             </blockquote>

						</section>
						</div>

                    <?php endwhile;	?>



					</div>

			</section>
					</header>

			</section>

	</body>
</html>
