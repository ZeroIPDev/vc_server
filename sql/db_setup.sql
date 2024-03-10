DROP TABLE IF EXISTS `vc_accounts`;
CREATE TABLE `vc_accounts` (
  `ID` int AUTO_INCREMENT PRIMARY KEY,
  `Name` varchar(17) NOT NULL,
  `Pass` char(9) NOT NULL,
  `Steam` varchar(32),
  `PlayerLevel` varchar(64) NOT NULL DEFAULT '1,0,1000',
  `Casual` varchar(999) NOT NULL DEFAULT '0,0',
  `CoopDiff` tinyint NOT NULL DEFAULT 0,
  `Boosts` varchar(20) NOT NULL DEFAULT '0,0,0,0,0',
  `Revives` int NOT NULL DEFAULT 0,
  `Icons` text,
  `Event` text,
  `Daily` datetime NOT NULL DEFAULT '1999-01-01 00:00:00'
) ENGINE=MyISAM  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `vc_daily`;
CREATE TABLE `vc_daily` (
    `ID` int AUTO_INCREMENT PRIMARY KEY,
    `Task1` varchar(32) NOT NULL,
    `Task2` varchar(32) NOT NULL,
    `Task3` varchar(32) NOT NULL,
    `Task4` varchar(32) NOT NULL,
    `Task5` varchar(32) NOT NULL,
    `Reward` varchar(32) NOT NULL
) ENGINE=MyISAM  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO `vc_daily` (`Task1`, `Task2`, `Task3`, `Task4`, `Task5`, `Reward`) VALUES ('0', '0', '0', '0', '0', '0');

DROP TABLE IF EXISTS `vc_events`;
CREATE TABLE `vc_events` (
    `ID` int AUTO_INCREMENT PRIMARY KEY,
    `Name` varchar(255) NOT NULL,
    `Description` text NOT NULL,
    `Data` varchar(255) NOT NULL,
    `Reward` varchar(255),
    `Start` datetime NOT NULL,
    `End` datetime NOT NULL
) ENGINE=MyISAM  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `vc_missions`;
CREATE TABLE `vc_missions` (
    `ID` int AUTO_INCREMENT PRIMARY KEY,
    `TaskHandle` varchar(255) NOT NULL,
    `Tasks` varchar(255) NOT NULL
) ENGINE=MyISAM  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO `vc_missions` (`TaskHandle`, `Tasks`) VALUES ('Stub', '0,10;1,10;2,10');

DROP TABLE IF EXISTS `vc_requests`;
CREATE TABLE `vc_requests` (
    `ID` int AUTO_INCREMENT PRIMARY KEY,
    `Sender` varchar(17) NOT NULL,
    `User` varchar(17) NOT NULL
) ENGINE=MyISAM  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
