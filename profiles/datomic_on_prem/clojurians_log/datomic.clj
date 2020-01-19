(ns clojurians-log.datomic
  (:require [datomic.api :as d]))

(def create-database d/create-database)
(def connect d/connect)
(def db d/db)
(def q d/q)
(def transact d/transact)
(def transact-async d/transact-async)
