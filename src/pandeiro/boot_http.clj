(ns pandeiro.boot-http
  {:boot/export-tasks true}
  (:require
   [boot.pod           :as pod]
   [boot.util          :as util]
   [boot.core          :as core :refer [deftask]]
   [boot.task.built-in :as task]))

(def default-port 3000)

(def serve-deps
  '[[ring/ring-jetty-adapter "1.3.1"]
    [ring/ring-core "1.3.1"]])

(deftask serve
  "Start a web server on localhost, serving resources and optionally a directory.
  Listens on port 3000 by default."

  [d dir     PATH str "The directory to serve."
   H handler SYM  sym "The ring handler to serve."
   r resource-root ROOT str "The root prefix when serving resources from classpath"
   p port    PORT int "The port to listen on. (Default: 3000)"]

  (let [port   (or port default-port)
        worker (pod/make-pod (update-in (core/get-env) [:dependencies]
                                        into serve-deps))
        start  (delay
                 (pod/with-eval-in worker
                   (require '[pandeiro.boot-http.impl :as http])
                   (def server
                     (http/server {:dir ~dir, :port ~port, :handler '~handler
                                   :resource-root ~resource-root})))
                 (util/info "<< started Jetty on http://localhost:%d >>\n" port))]
    (core/cleanup
      (util/info "\n<< stopping Jetty... >>\n")
      (pod/with-eval-in worker
        (.stop server)))
    (core/with-pre-wrap fileset
      @start
      fileset)))
