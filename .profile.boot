;; 예외를 좀더 이쁘게 
(alter-var-root
 #'boot.from.io.aviso.exception/*traditional*
 (constantly true))
;; 컬러링 처리
(alter-var-root
 #'boot.from.io.aviso.exception/*fonts*
 assoc :message boot.from.io.aviso.ansi/white-font)
