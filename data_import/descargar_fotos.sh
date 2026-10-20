#!/usr/bin/env bash
# Baja la foto de cada lugar listada en data_import/fotos_lugares.txt (Wikimedia Commons, 960 px)
# a una carpeta temporal. Luego data_import/reducir_fotos.ps1 las deja en
# app/src/main/assets/figma/img/place_<id>.jpg (900 px, JPEG).
#
#   bash data_import/descargar_fotos.sh <carpeta temporal>
set -u
here="$(cd "$(dirname "$0")" && pwd)"
out="${1:?carpeta de descarga}"
mkdir -p "$out"
ua="U-Spot-student-app/1.0 (educational project)"
grep -v '^#' "$here/fotos_lugares.txt" | while IFS='|' read -r id path _rest; do
  id="$(echo "$id" | xargs)"; path="$(echo "$path" | xargs)"
  [ -z "$id" ] && continue
  case "$path" in LOCAL:*) continue ;; esac
  if [ -s "$out/$id.img" ]; then continue; fi
  if [[ "$path" == https://* ]]; then url="$path"
  else url="https://thumb.wikimedia.org/wikipedia/commons/thumb/$path/960px-${path##*/}"; fi
  for try in 1 2 3 4; do
    code=$(curl -sL -A "$ua" -o "$out/$id.img" -w "%{http_code}" "$url")
    [ "$code" = "200" ] && break
    rm -f "$out/$id.img"; sleep $((try * 15))
  done
  echo "$code $id"
  sleep 1
done
echo FIN
