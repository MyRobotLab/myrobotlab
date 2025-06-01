gopro = runtime.start("gopro","GoPro")

#declare which kind of GoPro Family you want to control "HERO3", "HERO4"
gopro.setCameraModel("HERO4")

#insert the password of the GoPro Wifi
gopro.setWifiPassword("password")

#shutter On
gopro.shutterOn()
