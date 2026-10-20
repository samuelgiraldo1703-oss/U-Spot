# Fotos por categoría (assets/figma/img/cat_*.jpg)

Fotos reales tomadas de Wikimedia Commons, reducidas a 900 px de ancho. Sus licencias (CC BY / CC BY-SA)
permiten usarlas citando al autor; si la app se publica, estos créditos deben aparecer en la app.

| Archivo | Lugar | Autor | Licencia | Fuente |
|---|---|---|---|---|
| cat_restaurantes.jpg | La Puerta Falsa (Bogotá) | William Neuheisel | CC BY 2.0 | https://commons.wikimedia.org/wiki/File:Counter_at_La_Puerta_Falsa_(5618082838).jpg |
| cat_cafes.jpg | Café Pasaje (Bogotá) | Juan Carlos Pachón | CC BY-SA 2.0 | https://commons.wikimedia.org/wiki/File:ARTZ5301_CAFE_PASAJE_(15329279891).jpg |
| cat_bares.jpg | Bogotá Beer Company | Andres Rueda | CC BY 2.0 | https://commons.wikimedia.org/wiki/File:Bogota_Beer_Company.jpg |
| cat_teatros.jpg | Teatro Colón (Bogotá) | Ricardoinbaviera | CC BY-SA 3.0 | https://commons.wikimedia.org/wiki/File:Interior_Teatro_Colon_Bogota.jpg |
| cat_ocio.jpg | Funicular de Monserrate | Paul from Hacketts Cove | CC BY 2.0 | https://commons.wikimedia.org/wiki/File:Going_up!_Taking_the_funicular_to_Monserrate_(44695876020).jpg |
| cat_museos.jpg | Museo Botero (Bogotá) | Felipe Restrepo Acosta | CC BY-SA 3.0 | https://commons.wikimedia.org/wiki/File:Patio_Museo_Botero_Bogota.JPG |
| cat_discotecas.jpg | Club Tigerheat (no es de Bogotá: no hubo fotos libres de discotecas de la zona) | Lanisha Cole | CC BY-SA 4.0 | https://commons.wikimedia.org/wiki/File:Club_Tigerheat_on_the_Dance_Floor_with_Laser_Lights.jpg |
| cat_librerias.jpg | Librería Prosa del Mundo (Bogotá) | Alequihdez | CC BY-SA 4.0 | https://commons.wikimedia.org/wiki/File:Librer%C3%ADa_Prosa_del_Mundo_en_Bogot%C3%A1_03.jpg |

## Foto de cada lugar (assets/figma/img/place_<id>.jpg)

Cada lugar del Excel sin pantalla de Figma tiene su propia foto (ninguna se repite). El autor y la
licencia de cada una están en `fotos_lugares.txt` (columna 5: 1 = foto del local real, 0 = foto de
referencia del mismo tipo de lugar). Para cambiar una foto: editar su línea, correr
`bash data_import/descargar_fotos.sh <carpeta>`, `reducir_fotos.ps1 <carpeta>` y `excel_to_json.ps1`.
Las fotos `cat_*.jpg` de arriba quedan como respaldo por categoría.
