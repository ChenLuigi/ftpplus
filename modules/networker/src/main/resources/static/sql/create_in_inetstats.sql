-- phpMyAdmin SQL Dump
-- version 4.4.15.10
-- https://www.phpmyadmin.net
--
-- Хост: localhost
-- Время создания: Сен 08 2019 г., 10:45
-- Версия сервера: 5.7.23-24
-- Версия PHP: 5.3.28

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

--
-- База данных: `inetstats`
--

-- --------------------------------------------------------

--
-- Структура таблицы `test`
--

CREATE TABLE IF NOT EXISTS `usersip` (
  `idrec` mediumint(11) unsigned NOT NULL COMMENT '',
  `stamp` bigint(13) unsigned NOT NULL COMMENT '',
  `ip` varchar(20) NOT NULL COMMENT '',
  `bytes` int(11) NOT NULL COMMENT '',
  `site` varchar(254) NOT NULL COMMENT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Индексы сохранённых таблиц
--

--
-- Индексы таблицы `test`
--
ALTER TABLE `usersip`
  ADD PRIMARY KEY (`idrec`),
  ADD UNIQUE KEY `stamp` (`stamp`,`ip`,`bytes`) USING BTREE,
  ADD KEY `ip` (`ip`);

--
-- AUTO_INCREMENT для сохранённых таблиц
--

--
-- AUTO_INCREMENT для таблицы `test`
--
ALTER TABLE `usersip`
  MODIFY `idrec` mediumint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '';

--
-- Добавим колонку
--
ALTER TABLE `usersip` ADD `timespend` INT UNSIGNED NOT NULL AFTER `bytes`;