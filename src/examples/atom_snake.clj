; Inspired by the snakes the have gone before:
; Abhishek Reddy's snake: http://www.plt1.com/1070/even-smaller-snake/
; Mark Volkmann's snake: http://www.ociweb.com/mark/programming/ClojureSnake.html 

(ns examples.atom-snake
  (:import (java.awt Color Dimension) 
	   (javax.swing JPanel JFrame Timer JOptionPane)
           (java.awt.event ActionListener KeyListener))
  (:use clojure.contrib.import-static
	[clojure.contrib.seq-utils :only (includes?)]))
(import-static java.awt.event.KeyEvent VK_LEFT VK_RIGHT VK_UP VK_DOWN)

; ----------------------------------------------------------
; functional model
; ----------------------------------------------------------
(def width 75)
(def height 50)
(def point-size 10)
(def turn-millis 75)
(def win-length 5)
(def dirs { VK_LEFT  [-1  0] 
            VK_RIGHT [ 1  0]
            VK_UP    [ 0 -1] 
            VK_DOWN  [ 0  1]})

(defn add-points [& pts] 
  (vec (apply map + pts)))

(defn point-to-screen-rect [pt] 
  (map #(* point-size %) 
       [(pt 0) (pt 1) 1 1]))

(defn create-apple [] 
  {:location [(rand-int width) (rand-int height)]
   :color (Color. 210 50 90)
   :type :apple}) 

(defn create-snake []
  {:body (list [1 1]) 
   :dir [1 0]
   :type :snake
   :color (Color. 15 160 70)})

(defn move [{:keys [body dir] :as snake} & grow]
  (assoc snake :body (cons (add-points (first body) dir) 
			   (if grow body (butlast body)))))

(defn turn [snake newdir] 
  (if newdir (assoc snake :dir newdir) snake))

(defn border? [{[snake-head] :body, d :dir}]
  (let [[x y] (add-points snake-head d)]
    (or (= x 0) (= x width) (= y 0) (= y height))
    )
  )
(defn win? [{body :body}]
  (>= (count body) win-length))

(defn head-overlaps-body? [{[head & body] :body}]
  (includes? body head))

(def lose? head-overlaps-body?) 
(def left-m [
             [0 -1]
             [1 0]])

(defn eats? [{[snake-head] :body} {apple :location}]
   (= snake-head apple))
(defn opposite-dir? [d1 d2]
  (= 0 (apply + (map #(if (> 0 %) % (- %)) (map + d1 d2)))))
  

(defn update-direction [{snake :snake :as game} newdir]
  (let [dir (snake :dir)]
    (if-not (opposite-dir? dir newdir)
      (merge game {:snake (turn snake newdir)})
      game)))

(defn mult-mv [m v]
  (map #(apply + %) (map #(map * % v) m)
  ))
(defn turn-left [dir]
  (mult-mv left-m dir))

; START: update-positions
(defn update-positions [{snake :snake, apple :apple, :as game}]
  (if (eats? snake apple)
    (merge game {:apple (create-apple) :snake (move snake :grow)})
    (merge game {:snake (move snake)})
    ))
; END: update-positions


(defn reset-game [game]
  (merge game {:apple (create-apple) :snake (create-snake)}))
; ----------------------------------------------------------
; gui
; ----------------------------------------------------------
(defn fill-point [g pt color] 
  (let [[x y width height] (point-to-screen-rect pt)]
    (.setColor g color) 
    (.fillRect g x y width height)))

(defmulti paint (fn [g object & _] (:type object)))

(defmethod paint :apple [g {:keys [location color]}] 
  (fill-point g location color))

(defmethod paint :snake [g {:keys [body color]}] 
  (doseq [point body]
    (fill-point g point color)))

(defn game-panel [frame game]
  (proxy [JPanel ActionListener KeyListener] []
    (paintComponent [g] 
      (proxy-super paintComponent g)
      (paint g (@game :snake))
      (paint g (@game :apple)))
    ; START: swap!
    (actionPerformed [e] 
      (when (border? (@game :snake)) (swap! game update-direction (turn-left ((@game :snake) :dir))))

      (swap! game update-positions )
      (when (lose? (@game :snake))
        (swap! game reset-game)

	(JOptionPane/showMessageDialog frame "You lose!"))
    ; END: swap!

    (when (win? (@game :snake))
        (swap! game reset-game)
	(JOptionPane/showMessageDialog frame "You win!"))
      (.repaint this))
    (keyPressed [e] 
      (swap! game update-direction (dirs (.getKeyCode e))))
    (getPreferredSize [] 
      (Dimension. (* (inc width) point-size) 
		  (* (inc height) point-size)))
    (keyReleased [e])
    (keyTyped [e])))

(defn game [] 
  (let [game (atom (reset-game {}))
        frame (JFrame. "Snake")
        panel (game-panel frame game)
        timer (Timer. turn-millis panel)]
    (doto panel 
      (.setFocusable true)
      (.addKeyListener panel))
    (doto frame 
      (.add panel)
      (.pack)
      (.setVisible true))
    (.start timer) 
    [game, timer])) 



