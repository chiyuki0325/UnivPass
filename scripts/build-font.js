import Fontmin from "fontmin"

const fontmin = new Fontmin()
  .src('/home/chiyuki/下载/Roboto.ttf')
  .dest('.')
  .use(
    Fontmin.glyph({
      text: '01234566789:.'
    })
  )


fontmin.run(function (err, files) {
  if (err) {
    throw err
  }

  console.log(files[0]);
})
