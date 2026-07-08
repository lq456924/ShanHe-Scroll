-- MySQL dump 10.13  Distrib 9.5.0, for Win64 (x86_64)
--
-- Host: localhost    Database: my_travel
-- ------------------------------------------------------
-- Server version	9.5.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `album`
--

DROP TABLE IF EXISTS `album`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `album` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `region_id` bigint NOT NULL COMMENT '关联地区ID（城市/区）',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '相册标题',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '相册描述',
  `cover_image` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图片URL',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `visibility` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_album_user` (`user_id`),
  KEY `idx_album_user_region` (`user_id`,`region_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='相册表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `album`
--

LOCK TABLES `album` WRITE;
/*!40000 ALTER TABLE `album` DISABLE KEYS */;
/*!40000 ALTER TABLE `album` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `album_photo`
--

DROP TABLE IF EXISTS `album_photo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `album_photo` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `album_id` bigint NOT NULL COMMENT '所属相册ID',
  `url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片URL',
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片描述',
  `sort_order` int DEFAULT NULL COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_photo_album` (`album_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='相册图片表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `album_photo`
--

LOCK TABLES `album_photo` WRITE;
/*!40000 ALTER TABLE `album_photo` DISABLE KEYS */;
/*!40000 ALTER TABLE `album_photo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `album_visibility_user`
--

DROP TABLE IF EXISTS `album_visibility_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `album_visibility_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `album_id` bigint NOT NULL COMMENT '相册ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `type` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_avu_album` (`album_id`),
  KEY `idx_avu_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='相册可见性用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `album_visibility_user`
--

LOCK TABLES `album_visibility_user` WRITE;
/*!40000 ALTER TABLE `album_visibility_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `album_visibility_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attraction`
--

DROP TABLE IF EXISTS `attraction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attraction` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '景点ID',
  `region_id` bigint NOT NULL COMMENT '所属城市/区ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '景点名称',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '景点详细介绍',
  `image` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图片路径',
  `address` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '详细地址',
  `rating` decimal(38,2) DEFAULT NULL,
  `recommend_month` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '推荐游玩月份',
  `tips` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '游玩提示',
  `like_count` int DEFAULT NULL,
  `reviewed` int NOT NULL,
  `submitter_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_region_id` (`region_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attraction`
--

LOCK TABLES `attraction` WRITE;
/*!40000 ALTER TABLE `attraction` DISABLE KEYS */;
INSERT INTO `attraction` VALUES (1,2701,'兵马俑','兵马俑，即秦始皇兵马俑，是第一批全国重点文物保护单位、第一批中国世界遗产，位于陕西省西安市临潼区秦始皇陵以东1.5千米处的兵马俑坑内。兵马俑是古代墓葬雕塑的一个类别，秦始皇兵马俑被誉为\"世界第八大奇迹\"。',NULL,'陕西省西安市临潼区秦陵北路',4.80,'3-5月,9-10月','建议提前网上预约门票，避开节假日高峰；参观顺序建议一号坑→三号坑→二号坑',0,1,NULL);
/*!40000 ALTER TABLE `attraction` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attraction_image`
--

DROP TABLE IF EXISTS `attraction_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attraction_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `attraction_id` bigint NOT NULL COMMENT '所属景点ID',
  `url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片路径',
  `sort_order` int DEFAULT '0' COMMENT '排序（越小越靠前）',
  PRIMARY KEY (`id`),
  KEY `idx_attraction_id` (`attraction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点图片表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attraction_image`
--

LOCK TABLES `attraction_image` WRITE;
/*!40000 ALTER TABLE `attraction_image` DISABLE KEYS */;
/*!40000 ALTER TABLE `attraction_image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bottle_like`
--

DROP TABLE IF EXISTS `bottle_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bottle_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bottle_id` bigint NOT NULL COMMENT '漂流瓶ID',
  `user_id` bigint NOT NULL COMMENT '点赞用户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_like_bottle_user` (`bottle_id`,`user_id`),
  UNIQUE KEY `UK1vrdt1ry9i8hhqrwb2h0lmvib` (`bottle_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='漂流瓶点赞表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bottle_like`
--

LOCK TABLES `bottle_like` WRITE;
/*!40000 ALTER TABLE `bottle_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `bottle_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `drift_bottle`
--

DROP TABLE IF EXISTS `drift_bottle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `drift_bottle` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sender_id` bigint NOT NULL COMMENT '发送者用户ID',
  `is_anonymous` int NOT NULL,
  `text_content` text COLLATE utf8mb4_unicode_ci COMMENT '文字内容',
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片URL',
  `status` int NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  PRIMARY KEY (`id`),
  KEY `idx_bottle_sender` (`sender_id`),
  KEY `idx_bottle_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='漂流瓶表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `drift_bottle`
--

LOCK TABLES `drift_bottle` WRITE;
/*!40000 ALTER TABLE `drift_bottle` DISABLE KEYS */;
/*!40000 ALTER TABLE `drift_bottle` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `region`
--

DROP TABLE IF EXISTS `region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `region` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '地区ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父级ID，顶级为NULL',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '地区名称',
  `level` int NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '简介',
  `image` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图片路径',
  `sort_order` int DEFAULT '0' COMMENT '排序（越小越靠前）',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_level` (`level`)
) ENGINE=InnoDB AUTO_INCREMENT=3162 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地区表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `region`
--

LOCK TABLES `region` WRITE;
/*!40000 ALTER TABLE `region` DISABLE KEYS */;
INSERT INTO `region` VALUES (1,NULL,'中国',1,'中华人民共和国',NULL,1),(2,1,'北京',2,'首都，直辖市',NULL,1),(3,1,'天津',2,'直辖市',NULL,2),(4,1,'上海',2,'直辖市，国际化大都市',NULL,3),(5,1,'重庆',2,'直辖市，山城',NULL,4),(6,1,'河北',2,'燕赵之地',NULL,5),(7,1,'山西',2,'表里山河',NULL,6),(8,1,'内蒙古',2,'草原风光',NULL,7),(9,1,'辽宁',2,'辽沈大地',NULL,8),(10,1,'吉林',2,'关东大地',NULL,9),(11,1,'黑龙江',2,'北国风光',NULL,10),(12,1,'江苏',2,'鱼米之乡',NULL,11),(13,1,'浙江',2,'诗画江南',NULL,12),(14,1,'安徽',2,'徽风皖韵',NULL,13),(15,1,'福建',2,'八闽大地',NULL,14),(16,1,'江西',2,'赣鄱大地',NULL,15),(17,1,'山东',2,'齐鲁之乡',NULL,16),(18,1,'河南',2,'中原大地',NULL,17),(19,1,'湖北',2,'荆楚大地',NULL,18),(20,1,'湖南',2,'潇湘大地',NULL,19),(21,1,'广东',2,'岭南大地',NULL,20),(22,1,'广西',2,'八桂大地',NULL,21),(23,1,'海南',2,'热带海岛',NULL,22),(24,1,'四川',2,'天府之国',NULL,23),(25,1,'贵州',2,'多彩贵州',NULL,24),(26,1,'云南',2,'彩云之南',NULL,25),(27,1,'西藏',2,'雪域高原',NULL,26),(28,1,'陕西',2,'三秦大地',NULL,27),(29,1,'甘肃',2,'丝路风光',NULL,28),(30,1,'青海',2,'大美青海',NULL,29),(31,1,'宁夏',2,'塞上江南',NULL,30),(32,1,'新疆',2,'西域风情',NULL,31),(33,1,'台湾',2,'宝岛台湾',NULL,32),(34,1,'香港',2,'东方之珠',NULL,33),(35,1,'澳门',2,'海上花园',NULL,34),(101,2,'东城区',3,'故宫、天安门所在地',NULL,1),(102,2,'西城区',3,'北海公园、恭王府所在地',NULL,2),(103,2,'朝阳区',3,'CBD核心区',NULL,3),(104,2,'海淀区',3,'高校与科技园区聚集地',NULL,4),(105,2,'丰台区',3,'北京南城',NULL,5),(106,2,'石景山区',3,'首钢园所在地',NULL,6),(107,2,'通州区',3,'北京城市副中心',NULL,7),(108,2,'大兴区',3,'大兴机场所在地',NULL,8),(109,2,'昌平区',3,'明十三陵所在地',NULL,9),(110,2,'顺义区',3,'首都机场所在地',NULL,10),(111,2,'房山区',3,'周口店遗址所在地',NULL,11),(112,2,'门头沟区',3,'西山风景区',NULL,12),(113,2,'平谷区',3,'桃乡',NULL,13),(114,2,'怀柔区',3,'雁栖湖、慕田峪长城所在地',NULL,14),(115,2,'密云区',3,'密云水库、古北水镇所在地',NULL,15),(116,2,'延庆区',3,'八达岭长城所在地',NULL,16),(201,3,'和平区',3,'天津核心城区',NULL,1),(202,3,'河东区',3,'天津东部城区',NULL,2),(203,3,'河西区',3,'天津西部城区',NULL,3),(204,3,'南开区',3,'天津老城区',NULL,4),(205,3,'河北区',3,'天津北部城区',NULL,5),(206,3,'滨海新区',3,'天津港所在地',NULL,6),(207,3,'蓟州区',3,'盘山、独乐寺所在地',NULL,7),(301,4,'黄浦区',3,'外滩、南京路所在地',NULL,1),(302,4,'徐汇区',3,'衡山路、武康路所在地',NULL,2),(303,4,'长宁区',3,'中山公园所在地',NULL,3),(304,4,'静安区',3,'静安寺所在地',NULL,4),(305,4,'普陀区',3,'苏州河沿岸',NULL,5),(306,4,'虹口区',3,'多伦路文化名人街所在地',NULL,6),(307,4,'杨浦区',3,'大学聚集区',NULL,7),(308,4,'浦东新区',3,'陆家嘴、迪士尼所在地',NULL,8),(309,4,'闵行区',3,'七宝古镇所在地',NULL,9),(310,4,'松江区',3,'佘山、广富林所在地',NULL,10),(311,4,'青浦区',3,'朱家角古镇所在地',NULL,11),(312,4,'奉贤区',3,'奉贤海湾所在地',NULL,12),(313,4,'崇明区',3,'崇明岛所在地',NULL,13),(401,5,'渝中区',3,'解放碑、洪崖洞所在地',NULL,1),(402,5,'江北区',3,'观音桥商圈所在地',NULL,2),(403,5,'南岸区',3,'南山一棵树观景台所在地',NULL,3),(404,5,'沙坪坝区',3,'磁器口所在地',NULL,4),(405,5,'九龙坡区',3,'重庆动物园所在地',NULL,5),(406,5,'渝北区',3,'江北机场所在地',NULL,6),(407,5,'万州区',3,'三峡库区中心城市',NULL,7),(408,5,'涪陵区',3,'榨菜之乡',NULL,8),(409,5,'武隆区',3,'天生三桥所在地',NULL,9),(501,6,'石家庄',3,'河北省会',NULL,1),(502,6,'唐山',3,'京津冀工业重镇',NULL,2),(503,6,'秦皇岛',3,'海滨旅游城市',NULL,3),(504,6,'邯郸',3,'三千年古都',NULL,4),(505,6,'邢台',3,'古都邢州',NULL,5),(506,6,'保定',3,'京畿重镇',NULL,6),(507,6,'张家口',3,'塞外山城，冬奥之城',NULL,7),(508,6,'承德',3,'避暑山庄所在地',NULL,8),(509,6,'沧州',3,'武术之乡',NULL,9),(510,6,'廊坊',3,'京津走廊',NULL,10),(511,6,'衡水',3,'衡水湖所在地',NULL,11),(601,7,'太原',3,'山西省会，龙城',NULL,1),(602,7,'大同',3,'云冈石窟所在地',NULL,2),(603,7,'阳泉',3,'山城',NULL,3),(604,7,'长治',3,'太行山水',NULL,4),(605,7,'晋城',3,'太行明珠',NULL,5),(606,7,'朔州',3,'雁门关外',NULL,6),(607,7,'晋中',3,'平遥古城所在地',NULL,7),(608,7,'运城',3,'关公故里',NULL,8),(609,7,'忻州',3,'五台山所在地',NULL,9),(610,7,'临汾',3,'壶口瀑布所在地',NULL,10),(611,7,'吕梁',3,'革命老区',NULL,11),(701,8,'呼和浩特',3,'内蒙古首府，青城',NULL,1),(702,8,'包头',3,'草原钢城',NULL,2),(703,8,'乌海',3,'黄河明珠',NULL,3),(704,8,'赤峰',3,'草原之城',NULL,4),(705,8,'通辽',3,'科尔沁草原',NULL,5),(706,8,'鄂尔多斯',3,'成吉思汗陵所在地',NULL,6),(707,8,'呼伦贝尔',3,'呼伦贝尔大草原',NULL,7),(708,8,'巴彦淖尔',3,'河套平原',NULL,8),(709,8,'乌兰察布',3,'草原云谷',NULL,9),(710,8,'兴安盟',3,'阿尔山所在地',NULL,10),(711,8,'锡林郭勒盟',3,'锡林郭勒大草原',NULL,11),(712,8,'阿拉善盟',3,'阿拉善沙漠，胡杨林',NULL,12),(801,9,'沈阳',3,'辽宁省会，盛京',NULL,1),(802,9,'大连',3,'浪漫之都，海滨城市',NULL,2),(803,9,'鞍山',3,'千山所在地',NULL,3),(804,9,'抚顺',3,'雷锋之城',NULL,4),(805,9,'本溪',3,'枫叶之都',NULL,5),(806,9,'丹东',3,'鸭绿江畔，边境城市',NULL,6),(807,9,'锦州',3,'辽西走廊',NULL,7),(808,9,'营口',3,'渤海明珠',NULL,8),(809,9,'阜新',3,'玉龙故乡',NULL,9),(810,9,'辽阳',3,'东北第一城',NULL,10),(811,9,'盘锦',3,'红海滩所在地',NULL,11),(812,9,'铁岭',3,'小品之乡',NULL,12),(813,9,'朝阳',3,'鸟化石故乡',NULL,13),(814,9,'葫芦岛',3,'关外第一市',NULL,14),(901,10,'长春',3,'吉林省会，电影之城',NULL,1),(902,10,'吉林',3,'雾凇之都',NULL,2),(903,10,'四平',3,'英雄之城',NULL,3),(904,10,'辽源',3,'梅花鹿之乡',NULL,4),(905,10,'通化',3,'葡萄酒之乡',NULL,5),(906,10,'白山',3,'长白山所在地',NULL,6),(907,10,'松原',3,'查干湖所在地',NULL,7),(908,10,'白城',3,'鹤乡',NULL,8),(909,10,'延边朝鲜族自治州',3,'朝鲜族风情',NULL,9),(1001,11,'哈尔滨',3,'黑龙江省会，冰城',NULL,1),(1002,11,'齐齐哈尔',3,'鹤城',NULL,2),(1003,11,'鸡西',3,'煤城',NULL,3),(1004,11,'鹤岗',3,'煤城',NULL,4),(1005,11,'双鸭山',3,'煤城',NULL,5),(1006,11,'大庆',3,'石油之都',NULL,6),(1007,11,'伊春',3,'林都',NULL,7),(1008,11,'佳木斯',3,'华夏东极',NULL,8),(1009,11,'七台河',3,'煤城',NULL,9),(1010,11,'牡丹江',3,'镜泊湖所在地',NULL,10),(1011,11,'黑河',3,'中俄边境城市',NULL,11),(1012,11,'绥化',3,'寒地黑土',NULL,12),(1013,11,'大兴安岭地区',3,'林海雪原',NULL,13),(1101,12,'南京',3,'江苏省会，六朝古都',NULL,1),(1102,12,'无锡',3,'太湖明珠',NULL,2),(1103,12,'徐州',3,'两汉文化发源地',NULL,3),(1104,12,'常州',3,'中华恐龙园所在地',NULL,4),(1105,12,'苏州',3,'园林之城，人间天堂',NULL,5),(1106,12,'南通',3,'近代第一城',NULL,6),(1107,12,'连云港',3,'山海相拥',NULL,7),(1108,12,'淮安',3,'淮扬菜之乡',NULL,8),(1109,12,'盐城',3,'湿地之都',NULL,9),(1110,12,'扬州',3,'烟花三月下扬州',NULL,10),(1111,12,'镇江',3,'金山寺所在地',NULL,11),(1112,12,'泰州',3,'梅兰芳故乡',NULL,12),(1113,12,'宿迁',3,'项王故里',NULL,13),(1201,13,'杭州',3,'浙江省会，人间天堂',NULL,1),(1202,13,'宁波',3,'港口城市，天一阁所在地',NULL,2),(1203,13,'温州',3,'民营经济之都',NULL,3),(1204,13,'嘉兴',3,'南湖、乌镇所在地',NULL,4),(1205,13,'湖州',3,'南浔古镇、莫干山所在地',NULL,5),(1206,13,'绍兴',3,'鲁迅故里，水乡之城',NULL,6),(1207,13,'金华',3,'横店影视城所在地',NULL,7),(1208,13,'衢州',3,'南孔圣地',NULL,8),(1209,13,'舟山',3,'普陀山、海天佛国',NULL,9),(1210,13,'台州',3,'天台山、神仙居所在地',NULL,10),(1211,13,'丽水',3,'秀山丽水，古堰画乡',NULL,11),(1301,14,'合肥',3,'安徽省会，大湖名城',NULL,1),(1302,14,'芜湖',3,'半城山半城水',NULL,2),(1303,14,'蚌埠',3,'淮河明珠',NULL,3),(1304,14,'淮南',3,'豆腐之乡',NULL,4),(1305,14,'马鞍山',3,'钢铁之城',NULL,5),(1306,14,'淮北',3,'煤城',NULL,6),(1307,14,'铜陵',3,'中国古铜都',NULL,7),(1308,14,'安庆',3,'黄梅戏之乡',NULL,8),(1309,14,'黄山',3,'天下第一奇山所在地',NULL,9),(1310,14,'滁州',3,'琅琊山、醉翁亭所在地',NULL,10),(1311,14,'阜阳',3,'皖北中心',NULL,11),(1312,14,'宿州',3,'灵璧奇石之乡',NULL,12),(1313,14,'六安',3,'大别山所在地',NULL,13),(1314,14,'亳州',3,'华佗故里，药都',NULL,14),(1315,14,'池州',3,'九华山所在地',NULL,15),(1316,14,'宣城',3,'文房四宝之乡',NULL,16),(1401,15,'福州',3,'福建省会，榕城',NULL,1),(1402,15,'厦门',3,'海上花园，鼓浪屿所在地',NULL,2),(1403,15,'莆田',3,'妈祖文化发源地',NULL,3),(1404,15,'三明',3,'泰宁大金湖所在地',NULL,4),(1405,15,'泉州',3,'海上丝绸之路起点',NULL,5),(1406,15,'漳州',3,'土楼所在地',NULL,6),(1407,15,'南平',3,'武夷山所在地',NULL,7),(1408,15,'龙岩',3,'古田会议会址所在地',NULL,8),(1409,15,'宁德',3,'太姥山所在地',NULL,9),(1501,16,'南昌',3,'江西省会，英雄城',NULL,1),(1502,16,'景德镇',3,'千年瓷都',NULL,2),(1503,16,'萍乡',3,'安源路矿所在地',NULL,3),(1504,16,'九江',3,'庐山所在地',NULL,4),(1505,16,'新余',3,'仙女湖所在地',NULL,5),(1506,16,'鹰潭',3,'龙虎山所在地',NULL,6),(1507,16,'赣州',3,'客家摇篮',NULL,7),(1508,16,'吉安',3,'井冈山所在地',NULL,8),(1509,16,'宜春',3,'明月山所在地',NULL,9),(1510,16,'抚州',3,'才子之乡',NULL,10),(1511,16,'上饶',3,'婺源、三清山所在地',NULL,11),(1601,17,'济南',3,'山东省会，泉城',NULL,1),(1602,17,'青岛',3,'帆船之都，海滨名城',NULL,2),(1603,17,'淄博',3,'齐国故都',NULL,3),(1604,17,'枣庄',3,'台儿庄古城所在地',NULL,4),(1605,17,'东营',3,'黄河入海口',NULL,5),(1606,17,'烟台',3,'仙境海岸，蓬莱所在地',NULL,6),(1607,17,'潍坊',3,'风筝之都',NULL,7),(1608,17,'济宁',3,'曲阜三孔所在地',NULL,8),(1609,17,'泰安',3,'泰山所在地',NULL,9),(1610,17,'威海',3,'最宜居海滨城市',NULL,10),(1611,17,'日照',3,'阳光海岸',NULL,11),(1612,17,'临沂',3,'沂蒙革命老区',NULL,12),(1613,17,'德州',3,'扒鸡之乡',NULL,13),(1614,17,'聊城',3,'江北水城',NULL,14),(1615,17,'滨州',3,'黄河三角洲',NULL,15),(1616,17,'菏泽',3,'牡丹之都',NULL,16),(1701,18,'郑州',3,'河南省会，交通枢纽',NULL,1),(1702,18,'开封',3,'八朝古都，清明上河园',NULL,2),(1703,18,'洛阳',3,'十三朝古都，龙门石窟所在地',NULL,3),(1704,18,'平顶山',3,'中原煤城',NULL,4),(1705,18,'安阳',3,'殷墟所在地',NULL,5),(1706,18,'鹤壁',3,'淇河文化',NULL,6),(1707,18,'新乡',3,'豫北中心',NULL,7),(1708,18,'焦作',3,'云台山所在地',NULL,8),(1709,18,'濮阳',3,'杂技之乡',NULL,9),(1710,18,'许昌',3,'曹魏故都',NULL,10),(1711,18,'漯河',3,'食品名城',NULL,11),(1712,18,'三门峡',3,'函谷关、三门峡大坝所在地',NULL,12),(1713,18,'南阳',3,'诸葛故里',NULL,13),(1714,18,'商丘',3,'商文化发源地',NULL,14),(1715,18,'信阳',3,'毛尖茶之乡',NULL,15),(1716,18,'周口',3,'伏羲故都',NULL,16),(1717,18,'驻马店',3,'嵖岈山所在地',NULL,17),(1718,18,'济源',3,'愚公故里',NULL,18),(1801,19,'武汉',3,'湖北省会，江城',NULL,1),(1802,19,'黄石',3,'矿冶之城',NULL,2),(1803,19,'十堰',3,'武当山所在地',NULL,3),(1804,19,'宜昌',3,'三峡大坝所在地',NULL,4),(1805,19,'襄阳',3,'三国文化名城',NULL,5),(1806,19,'鄂州',3,'湖北之东',NULL,6),(1807,19,'荆门',3,'绿色明珠',NULL,7),(1808,19,'孝感',3,'中华孝文化之乡',NULL,8),(1809,19,'荆州',3,'楚国故都',NULL,9),(1810,19,'黄冈',3,'东坡赤壁所在地',NULL,10),(1811,19,'咸宁',3,'温泉之都',NULL,11),(1812,19,'随州',3,'炎帝故里',NULL,12),(1813,19,'恩施土家族苗族自治州',3,'恩施大峡谷所在地',NULL,13),(1814,19,'仙桃',3,'体操之乡',NULL,14),(1815,19,'潜江',3,'水乡园林',NULL,15),(1816,19,'天门',3,'茶圣故里',NULL,16),(1817,19,'神农架林区',3,'神农架原始森林',NULL,17),(1901,20,'长沙',3,'湖南省会，星城',NULL,1),(1902,20,'株洲',3,'火车拉来的城市',NULL,2),(1903,20,'湘潭',3,'伟人故里',NULL,3),(1904,20,'衡阳',3,'南岳衡山所在地',NULL,4),(1905,20,'邵阳',3,'湘中宝庆',NULL,5),(1906,20,'岳阳',3,'洞庭天下水，岳阳天下楼',NULL,6),(1907,20,'常德',3,'桃花源所在地',NULL,7),(1908,20,'张家界',3,'张家界国家森林公园所在地',NULL,8),(1909,20,'益阳',3,'鱼米之乡',NULL,9),(1910,20,'郴州',3,'东江湖所在地',NULL,10),(1911,20,'永州',3,'潇湘之地',NULL,11),(1912,20,'怀化',3,'火车拖来的城市',NULL,12),(1913,20,'娄底',3,'湘中明珠',NULL,13),(1914,20,'湘西土家族苗族自治州',3,'凤凰古城所在地',NULL,14),(2001,21,'广州',3,'广东省会，花城',NULL,1),(2002,21,'韶关',3,'丹霞山所在地',NULL,2),(2003,21,'深圳',3,'创新之都',NULL,3),(2004,21,'珠海',3,'浪漫之城',NULL,4),(2005,21,'汕头',3,'经济特区，潮汕文化',NULL,5),(2006,21,'佛山',3,'武术之乡，顺德美食',NULL,6),(2007,21,'江门',3,'侨乡',NULL,7),(2008,21,'湛江',3,'海鲜之都',NULL,8),(2009,21,'茂名',3,'南方油城',NULL,9),(2010,21,'肇庆',3,'七星岩、鼎湖山所在地',NULL,10),(2011,21,'惠州',3,'西湖、罗浮山所在地',NULL,11),(2012,21,'梅州',3,'世界客都',NULL,12),(2013,21,'汕尾',3,'红海湾所在地',NULL,13),(2014,21,'河源',3,'万绿湖所在地',NULL,14),(2015,21,'阳江',3,'海陵岛、闸坡所在地',NULL,15),(2016,21,'清远',3,'漂流之乡',NULL,16),(2017,21,'东莞',3,'世界工厂',NULL,17),(2018,21,'中山',3,'孙中山故里',NULL,18),(2019,21,'潮州',3,'潮汕文化发源地',NULL,19),(2020,21,'揭阳',3,'揭阳楼所在地',NULL,20),(2021,21,'云浮',3,'石都',NULL,21),(2101,22,'南宁',3,'广西首府，绿城',NULL,1),(2102,22,'柳州',3,'龙城，螺蛳粉之乡',NULL,2),(2103,22,'桂林',3,'桂林山水甲天下',NULL,3),(2104,22,'梧州',3,'百年商埠',NULL,4),(2105,22,'北海',3,'涠洲岛所在地',NULL,5),(2106,22,'防城港',3,'北部湾明珠',NULL,6),(2107,22,'钦州',3,'钦州湾所在地',NULL,7),(2108,22,'贵港',3,'太平天国起义所在地',NULL,8),(2109,22,'玉林',3,'药材之都',NULL,9),(2110,22,'百色',3,'革命老区',NULL,10),(2111,22,'贺州',3,'黄姚古镇所在地',NULL,11),(2112,22,'河池',3,'巴马长寿之乡所在地',NULL,12),(2113,22,'来宾',3,'壮族文化',NULL,13),(2114,22,'崇左',3,'德天瀑布所在地',NULL,14),(2201,23,'海口',3,'海南省会，椰城',NULL,1),(2202,23,'三亚',3,'天涯海角，热带天堂',NULL,2),(2203,23,'三沙',3,'中国最南端地级市',NULL,3),(2204,23,'儋州',3,'东坡文化',NULL,4),(2301,24,'成都',3,'四川省会，天府之国的中心',NULL,1),(2302,24,'自贡',3,'千年盐都，恐龙之乡',NULL,2),(2303,24,'攀枝花',3,'阳光花城',NULL,3),(2304,24,'泸州',3,'酒城',NULL,4),(2305,24,'德阳',3,'三星堆所在地',NULL,5),(2306,24,'绵阳',3,'中国科技城',NULL,6),(2307,24,'广元',3,'女皇故里',NULL,7),(2308,24,'遂宁',3,'观音故里',NULL,8),(2309,24,'内江',3,'大千故里',NULL,9),(2310,24,'乐山',3,'乐山大佛所在地',NULL,10),(2311,24,'南充',3,'绸都',NULL,11),(2312,24,'眉山',3,'三苏故里',NULL,12),(2313,24,'宜宾',3,'五粮液之乡，蜀南竹海所在地',NULL,13),(2314,24,'广安',3,'小平故里',NULL,14),(2315,24,'达州',3,'川东门户',NULL,15),(2316,24,'雅安',3,'熊猫故乡',NULL,16),(2317,24,'巴中',3,'川陕革命根据地',NULL,17),(2318,24,'资阳',3,'陈毅故里',NULL,18),(2319,24,'阿坝藏族羌族自治州',3,'九寨沟、黄龙所在地',NULL,19),(2320,24,'甘孜藏族自治州',3,'稻城亚丁、色达所在地',NULL,20),(2321,24,'凉山彝族自治州',3,'泸沽湖、西昌卫星发射中心所在地',NULL,21),(2401,25,'贵阳',3,'贵州省会，避暑之都',NULL,1),(2402,25,'六盘水',3,'中国凉都',NULL,2),(2403,25,'遵义',3,'遵义会议会址所在地',NULL,3),(2404,25,'安顺',3,'黄果树瀑布所在地',NULL,4),(2405,25,'毕节',3,'百里杜鹃所在地',NULL,5),(2406,25,'铜仁',3,'梵净山所在地',NULL,6),(2407,25,'黔西南布依族苗族自治州',3,'万峰林所在地',NULL,7),(2408,25,'黔东南苗族侗族自治州',3,'西江千户苗寨所在地',NULL,8),(2409,25,'黔南布依族苗族自治州',3,'荔波小七孔所在地',NULL,9),(2501,26,'昆明',3,'云南省会，春城',NULL,1),(2502,26,'曲靖',3,'滇东门户',NULL,2),(2503,26,'玉溪',3,'抚仙湖所在地',NULL,3),(2504,26,'保山',3,'腾冲热海所在地',NULL,4),(2505,26,'昭通',3,'滇东北门户',NULL,5),(2506,26,'丽江',3,'丽江古城、玉龙雪山所在地',NULL,6),(2507,26,'普洱',3,'普洱茶故乡',NULL,7),(2508,26,'临沧',3,'世界佤乡',NULL,8),(2509,26,'楚雄彝族自治州',3,'元谋人遗址所在地',NULL,9),(2510,26,'红河哈尼族彝族自治州',3,'元阳梯田所在地',NULL,10),(2511,26,'文山壮族苗族自治州',3,'普者黑所在地',NULL,11),(2512,26,'西双版纳傣族自治州',3,'热带雨林，孔雀之乡',NULL,12),(2513,26,'大理白族自治州',3,'苍山洱海',NULL,13),(2514,26,'德宏傣族景颇族自治州',3,'瑞丽口岸所在地',NULL,14),(2515,26,'怒江傈僳族自治州',3,'三江并流所在地',NULL,15),(2516,26,'迪庆藏族自治州',3,'香格里拉所在地',NULL,16),(2601,27,'拉萨',3,'西藏首府，日光城',NULL,1),(2602,27,'日喀则',3,'珠穆朗玛峰所在地',NULL,2),(2603,27,'昌都',3,'藏东门户',NULL,3),(2604,27,'林芝',3,'西藏江南',NULL,4),(2605,27,'山南',3,'藏文化发源地',NULL,5),(2606,27,'那曲',3,'羌塘草原',NULL,6),(2607,27,'阿里地区',3,'冈仁波齐所在地',NULL,7),(2701,28,'西安',3,'陕西省会，十三朝古都',NULL,1),(2702,28,'铜川',3,'药王山所在地',NULL,2),(2703,28,'宝鸡',3,'青铜器之乡，法门寺所在地',NULL,3),(2704,28,'咸阳',3,'秦都所在地',NULL,4),(2705,28,'渭南',3,'华山所在地',NULL,5),(2706,28,'延安',3,'革命圣地',NULL,6),(2707,28,'汉中',3,'汉文化发源地',NULL,7),(2708,28,'榆林',3,'塞上名城',NULL,8),(2709,28,'安康',3,'秦巴山水',NULL,9),(2710,28,'商洛',3,'秦岭最美之地',NULL,10),(2801,29,'兰州',3,'甘肃省会，黄河之都',NULL,1),(2802,29,'嘉峪关',3,'天下第一雄关',NULL,2),(2803,29,'金昌',3,'镍都',NULL,3),(2804,29,'白银',3,'有色金属之都',NULL,4),(2805,29,'天水',3,'伏羲故里，麦积山石窟所在地',NULL,5),(2806,29,'武威',3,'马踏飞燕出土地',NULL,6),(2807,29,'张掖',3,'七彩丹霞所在地',NULL,7),(2808,29,'平凉',3,'崆峒山所在地',NULL,8),(2809,29,'酒泉',3,'敦煌莫高窟所在地',NULL,9),(2810,29,'庆阳',3,'岐黄故里',NULL,10),(2811,29,'定西',3,'马铃薯之乡',NULL,11),(2812,29,'陇南',3,'陇上江南',NULL,12),(2813,29,'临夏回族自治州',3,'中国小麦加',NULL,13),(2814,29,'甘南藏族自治州',3,'扎尕那、拉卜楞寺所在地',NULL,14),(2901,30,'西宁',3,'青海省会，夏都',NULL,1),(2902,30,'海东',3,'河湟文化',NULL,2),(2903,30,'海北藏族自治州',3,'青海湖所在地',NULL,3),(2904,30,'黄南藏族自治州',3,'热贡艺术之乡',NULL,4),(2905,30,'海南藏族自治州',3,'青海湖南岸',NULL,5),(2906,30,'果洛藏族自治州',3,'年保玉则所在地',NULL,6),(2907,30,'玉树藏族自治州',3,'三江源所在地',NULL,7),(2908,30,'海西蒙古族藏族自治州',3,'茶卡盐湖、恶魔之眼所在地',NULL,8),(3001,31,'银川',3,'宁夏首府，塞上湖城',NULL,1),(3002,31,'石嘴山',3,'贺兰山脚下',NULL,2),(3003,31,'吴忠',3,'黄河金岸',NULL,3),(3004,31,'固原',3,'六盘山所在地',NULL,4),(3005,31,'中卫',3,'沙坡头所在地',NULL,5),(3101,32,'乌鲁木齐',3,'新疆首府，亚心之都',NULL,1),(3102,32,'克拉玛依',3,'石油之城',NULL,2),(3103,32,'吐鲁番',3,'葡萄沟、火焰山所在地',NULL,3),(3104,32,'哈密',3,'哈密瓜之乡',NULL,4),(3105,32,'昌吉回族自治州',3,'天山天池所在地',NULL,5),(3106,32,'博尔塔拉蒙古自治州',3,'赛里木湖所在地',NULL,6),(3107,32,'巴音郭楞蒙古自治州',3,'博斯腾湖所在地',NULL,7),(3108,32,'阿克苏地区',3,'苹果之乡',NULL,8),(3109,32,'克孜勒苏柯尔克孜自治州',3,'帕米尔高原',NULL,9),(3110,32,'喀什地区',3,'喀什古城所在地',NULL,10),(3111,32,'和田地区',3,'和田玉故乡',NULL,11),(3112,32,'伊犁哈萨克自治州',3,'塞外江南，那拉提草原',NULL,12),(3113,32,'塔城地区',3,'边境口岸',NULL,13),(3114,32,'阿勒泰地区',3,'喀纳斯湖所在地',NULL,14),(3115,34,'中西区',3,'中环所在地',NULL,1),(3116,34,'湾仔区',3,'香港会议展览中心所在地',NULL,2),(3117,34,'东区',3,'铜锣湾所在地',NULL,3),(3118,34,'南区',3,'海洋公园所在地',NULL,4),(3119,34,'油尖旺区',3,'尖沙咀、旺角所在地',NULL,5),(3120,34,'深水埗区',3,'深水埗老街所在地',NULL,6),(3121,34,'九龙城区',3,'九龙城寨所在地',NULL,7),(3122,34,'黄大仙区',3,'黄大仙祠所在地',NULL,8),(3123,34,'观塘区',3,'观塘工业区所在地',NULL,9),(3124,34,'荃湾区',3,'荃湾新市镇',NULL,10),(3125,34,'屯门区',3,'屯门码头所在地',NULL,11),(3126,34,'元朗区',3,'元朗湿地公园所在地',NULL,12),(3127,34,'北区',3,'上水、粉岭所在地',NULL,13),(3128,34,'大埔区',3,'大埔海滨公园所在地',NULL,14),(3129,34,'西贡区',3,'西贡海鲜街所在地',NULL,15),(3130,34,'沙田区',3,'沙田马场所在地',NULL,16),(3131,34,'葵青区',3,'葵涌货柜码头所在地',NULL,17),(3132,34,'离岛区',3,'大屿山、迪士尼所在地',NULL,18),(3133,35,'花地玛堂区',3,'澳门半岛北部',NULL,1),(3134,35,'圣安多尼堂区',3,'澳门半岛西部',NULL,2),(3135,35,'大堂区',3,'澳门半岛中部',NULL,3),(3136,35,'望德堂区',3,'澳门半岛东部',NULL,4),(3137,35,'风顺堂区',3,'澳门半岛南部',NULL,5),(3138,35,'嘉模堂区',3,'氹仔',NULL,6),(3139,35,'圣方济各堂区',3,'路环',NULL,7),(3140,33,'台北市',3,'台湾省省会，政治经济中心',NULL,1),(3141,33,'新北市',3,'台湾最大城市',NULL,2),(3142,33,'桃园市',3,'桃园机场所在地',NULL,3),(3143,33,'台中市',3,'台湾中部中心',NULL,4),(3144,33,'台南市',3,'历史文化古城',NULL,5),(3145,33,'高雄市',3,'台湾最大港口城市',NULL,6),(3146,33,'基隆市',3,'台湾北部港口',NULL,7),(3147,33,'新竹市',3,'科技重镇',NULL,8),(3148,33,'嘉义市',3,'台湾南部中心城市',NULL,9),(3149,33,'新竹县',3,'竹科所在地',NULL,10),(3150,33,'苗栗县',3,'客家文化重镇',NULL,11),(3151,33,'彰化县',3,'台湾中部农业大县',NULL,12),(3152,33,'南投县',3,'日月潭所在地',NULL,13),(3153,33,'云林县',3,'农业大县',NULL,14),(3154,33,'嘉义县',3,'阿里山所在地',NULL,15),(3155,33,'屏东县',3,'垦丁所在地',NULL,16),(3156,33,'宜兰县',3,'温泉之乡',NULL,17),(3157,33,'花莲县',3,'太鲁阁所在地',NULL,18),(3158,33,'台东县',3,'台湾东部风景区',NULL,19),(3159,33,'澎湖县',3,'澎湖列岛',NULL,20),(3160,33,'金门县',3,'金门群岛',NULL,21),(3161,33,'连江县',3,'马祖列岛',NULL,22);
/*!40000 ALTER TABLE `region` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录用户名',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录密码（BCrypt加密）',
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像路径',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `gender` int DEFAULT NULL,
  `birthday` date DEFAULT NULL COMMENT '生日',
  `bio` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '个人简介',
  `location` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所在地区',
  `status` int DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `token_version` int DEFAULT '0' COMMENT 'token版本号，每次登录+1',
  `role` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-16  0:55:23
