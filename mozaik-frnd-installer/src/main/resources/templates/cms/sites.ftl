CREATE TABLE `${schema}`.`sites` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL,
  `domains` varchar(255) DEFAULT NULL,
  `descr` varchar(255) DEFAULT NULL,
  `alias` varchar(255) DEFAULT NULL,
  `index_page_id` int(11) DEFAULT NULL,
  `login_page_id` int(11) DEFAULT NULL,
  `404_page_id` int(11) DEFAULT NULL,
  `create_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `domain_UNIQUE` (`title`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8;
