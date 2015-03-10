# firebirdToPG

###Сборка

Приложение для миграции данных с субд firebird на postgresql.
Для сборки приложения нужен sbt версии 0.13.5

Для того чтобы запустить приложение в ОС, где не установлены исполняемые библиотеки scala,
необходимо собрать приложение в один jar файл, включая scala файлы:

```
$ sbt one-jar
```

###Запуск

После сборки с one-jar приложение можно запускать как обычную программу:

```
$ java -jar ./target/scala-2.11/firebirdtopg_2.11-1.0-one-jar.jar ./src/test/resources/userConfig.conf
```

Обратите внимание, для запуска приложения ему необходимо передать конфигурационный файл.

*Перед запуском программы в субд postgresql должна быть создана бд, в которую будут переноситься данные!*

###Пример файла конфигурации

Ниже перечислены параметры, необходимые для запуска приложения:

 - batchsize=300000//количество строк в пакете обработки обычной таблицы
 - blobBatchSize=100000//количество строк в пакете обработки таблицы, содержащей бинарные данные
 - global.numofworkers = 3// число акторов-обработчиков
 - global.numofworkers_squared = 9 // число акторов в квадрате - используем для коннекшн пула

 - database.name="archetype_db"
 - database.host="localhost"
 - db.default.driver="org.firebirdsql.jdbc.FBDriver"
 - db.default.url="jdbc:firebirdsql:"${database.host}"/3050:"${database.name}"?lc_ctype=WIN1251"
 - db.default.user="yourusername"
 - db.default.password="yourpassword"
 - db.default.isql="/opt/firebird/bin/isql" //путь до утилиты isql для получения sql метаданных бд
 -
 - db.pg.driver="org.postgresql.Driver"
 - db.pg.url="jdbc:postgresql://localhost:5432/"${database.name}
 - db.pg.user="yourusername"
 - db.pg.password="yourpassword"

Помимо этих параметров можно перекрывать параметры, указанные в /src/main/resources/application.conf

###Алгоритм работы программы

 - Используя *db.default.isql* мы получаем текстовое представление sql метаданных.
 - Преобразуем полученные данные (правила преобразования наглядно видны в src/test/scala/org/comsoft/SqlParserSpec.scala).
 - Получаем текущие значения сиквенсов/генератов в файрберд.
 - Создаем таблицы и сиквенсы в постгресе.
 - Начинаем миграцию данных (происходит параллельно).
 - Создаем внешние ключи и ограничения в постгресе.
