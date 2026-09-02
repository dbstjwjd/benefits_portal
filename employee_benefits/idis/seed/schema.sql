-- IDIS 스키마.
-- 운영은 ddl-auto=validate 라 애플리케이션이 테이블을 만들지 않는다.
-- 최초 배포에서 이 파일을 먼저 넣고 그 다음 prod-init.sql 을 넣는다.
--
--   mysql -u idis -p --default-character-set=utf8mb4 idis < seed/schema.sql
--
-- 엔티티를 고쳐 스키마가 바뀌면 로컬에서 아래로 다시 뽑는다.
-- 세 옵션을 반드시 넣을 것:
--   --set-gtid-purged=OFF   GTID 구문은 SUPER 권한이 필요하고 서버 UUID 가 박힌다
--   --skip-add-drop-table   DROP TABLE 이 남으면 재실행 시 운영 DB 가 날아간다
--   sed 로 AUTO_INCREMENT=n 제거 (로컬 카운터가 따라붙는다)
--
--   mysqldump -u root -p --no-data --skip-comments --single-transaction \
--     --set-gtid-purged=OFF --skip-add-drop-table idis \
--     | sed 's/ AUTO_INCREMENT=[0-9]*//' > seed/schema.sql
--
-- 이 파일은 CREATE TABLE 만 있어야 한다. 넣기 전에 확인:
--   grep -nE 'DROP|GTID|SQL_LOG_BIN|INSERT' seed/schema.sql   → 아무것도 안 나와야 한다



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
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `answer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `value` text,
  `question_id` bigint NOT NULL,
  `response_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8frr4bcabmmeyyu60qt7iiblo` (`question_id`),
  KEY `FKbr5pllrlyjpvhk4ldtst4mp23` (`response_id`),
  CONSTRAINT `FK8frr4bcabmmeyyu60qt7iiblo` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`),
  CONSTRAINT `FKbr5pllrlyjpvhk4ldtst4mp23` FOREIGN KEY (`response_id`) REFERENCES `response` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `answer_choice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `answer_id` bigint NOT NULL,
  `choice_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKkbfupx7h0niu8y6oo7vchditr` (`answer_id`,`choice_id`),
  KEY `FKsqwb29cjt7jyb0q4l630rt1xo` (`choice_id`),
  CONSTRAINT `FKhn8i8uuv27liqdccco5iu0yqm` FOREIGN KEY (`answer_id`) REFERENCES `answer` (`id`),
  CONSTRAINT `FKsqwb29cjt7jyb0q4l630rt1xo` FOREIGN KEY (`choice_id`) REFERENCES `choice` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `choice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(500) NOT NULL,
  `image_path` varchar(500) DEFAULT NULL,
  `sort_order` int NOT NULL,
  `question_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKcaq6r76cswke5b9fk6fyx3y5w` (`question_id`),
  CONSTRAINT `FKcaq6r76cswke5b9fk6fyx3y5w` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `department` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `name` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1t68827l97cwyxo9r1u6t4p7d` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee` (
  `emp_no` varchar(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `active` bit(1) NOT NULL,
  `name` varchar(50) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `role` enum('ADMIN','EMPLOYEE') NOT NULL,
  `type` enum('DIRECT','INDIRECT') NOT NULL,
  `department_id` bigint DEFAULT NULL,
  `hire_date` date DEFAULT NULL,
  `resign_date` date DEFAULT NULL,
  `super_admin` bit(1) NOT NULL,
  `pin_change_required` bit(1) NOT NULL,
  `pin_fail_count` int NOT NULL,
  `pin_hash` varchar(72) DEFAULT NULL,
  `pin_locked_until` datetime(6) DEFAULT NULL,
  `default_address` varchar(200) DEFAULT NULL,
  `default_address_detail` varchar(200) DEFAULT NULL,
  `default_zipcode` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`emp_no`),
  UNIQUE KEY `UKbuf2qp04xpwfp5qq355706h4a` (`phone`),
  KEY `FKbejtwvg9bxus2mffsm3swj3u9` (`department_id`),
  CONSTRAINT `FKbejtwvg9bxus2mffsm3swj3u9` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `form` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text,
  `end_at` datetime(6) DEFAULT NULL,
  `start_at` datetime(6) DEFAULT NULL,
  `status` enum('CLOSED','DRAFT','OPEN') NOT NULL,
  `target` enum('ALL','DIRECT','INDIRECT') NOT NULL,
  `title` varchar(200) NOT NULL,
  `created_by` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKlyi9g1aw1xfy9salmmb0oji4n` (`created_by`),
  CONSTRAINT `FKlyi9g1aw1xfy9salmmb0oji4n` FOREIGN KEY (`created_by`) REFERENCES `employee` (`emp_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `form_target_department` (
  `form_id` bigint NOT NULL,
  `department_id` bigint NOT NULL,
  PRIMARY KEY (`form_id`,`department_id`),
  KEY `FKk8ir9pukm3q7dcqesth0tov89` (`department_id`),
  CONSTRAINT `FKc0v3tw2vrf5jtlccprcy54ko4` FOREIGN KEY (`form_id`) REFERENCES `form` (`id`),
  CONSTRAINT `FKk8ir9pukm3q7dcqesth0tov89` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `config` json DEFAULT NULL,
  `required` bit(1) NOT NULL,
  `sort_order` int NOT NULL,
  `title` varchar(500) NOT NULL,
  `type` enum('ADDRESS','DATE','IMAGE_CHOICE','LONG_TEXT','MULTI_CHOICE','PHONE','SHORT_TEXT','SINGLE_CHOICE') NOT NULL,
  `form_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKfufwr8jclqwc3bw2d2tj4957f` (`form_id`),
  CONSTRAINT `FKfufwr8jclqwc3bw2d2tj4957f` FOREIGN KEY (`form_id`) REFERENCES `form` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `response` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `emp_no` varchar(20) NOT NULL,
  `form_id` bigint NOT NULL,
  `edited_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKs96nxsyc9t5olrqg9tqca390` (`form_id`,`emp_no`),
  KEY `FKl4g2r8r9bhfm01gfv189ixvhq` (`emp_no`),
  CONSTRAINT `FKl4g2r8r9bhfm01gfv189ixvhq` FOREIGN KEY (`emp_no`) REFERENCES `employee` (`emp_no`),
  CONSTRAINT `FKocqyk9kemf4uahjxuoctgdmia` FOREIGN KEY (`form_id`) REFERENCES `form` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `site_setting` (
  `setting_key` varchar(100) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `value` text,
  PRIMARY KEY (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

