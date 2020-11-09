(ns multimethods)

(defn foo [xxx])

(defmulti bar class)

(defmethod bar String [m]
  (str "IT's a a string: " m))

(defmethod bar Long [m]
  (str "IT's a number: " m))

public String bar (String x) ...
public String bar (Long x) ...

(bar "heello")
(bar 123)


(defmethod foo :type)

(defmethod foo :vegetable [m]
  [:VEG m])

(foo {:name "carrot"
      :type :vegetable})

(foo {:name "carrot"
      :type :fruit})
