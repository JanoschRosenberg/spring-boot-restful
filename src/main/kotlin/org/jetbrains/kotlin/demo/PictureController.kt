package org.jetbrains.kotlin.demo

import com.beust.klaxon.Klaxon
import com.google.common.base.Splitter
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicLong
import java.util.Base64
import khttp.delete as httpDelete
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO


@RestController
class PictureController {

    val counter = AtomicLong()

    @GetMapping(value = "/pic/{tx}", produces = arrayOf(MediaType.IMAGE_PNG_VALUE))
    fun pic(@RequestParam(value = "size", defaultValue = "5") size: String, @PathVariable("tx") tx: String): ByteArray {


        var iSize = 5
        if (size.matches("-?\\d+(\\.\\d+)?".toRegex())) {
            val si = Integer.valueOf(size)
            if (si >= 0 && si <= 20) {
                iSize = si
            }
        }

        val picMap = getPics().associateBy({ it.tx }, { it })

        val currentPic = picMap.get(tx)

        if (currentPic == null) {
            throw PicNotFoundException("not found")
        }

        val image = createImage(currentPic, iSize)
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "png", outputStream)

        return outputStream.toByteArray()
    }


    @GetMapping(value = "/pics", produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    fun pics(@RequestParam(value = "size", defaultValue = "5") size: String): List<String> {
        return getPics().map { it.tx }
    }

    fun createPallettes(context: String): ArrayList<Pallette> {
        val c = Klaxon()
                .parse<Container>(context)


        val pallettes = ArrayList<Pallette>()
        if (c?.confirmed != null) {
            pallettes.addAll(c.confirmed)
        }
        if (c?.unconfirmed != null) {
            pallettes.addAll(c.unconfirmed)
        }

        return pallettes
    }

    fun createImage(picture: Picture, size: Int): BufferedImage {
        val bi = BufferedImage(16 * size, 16 * size, BufferedImage.TYPE_INT_ARGB)
        val ig2 = bi.createGraphics()

        for (x in 0..15) {
            for (y in 0..15) {
                var index = x + y * 16
                ig2.color = picture.pallette?.colors?.get(Integer.parseInt(picture.picture[index] + "", 16))
                ig2.fillRect(x * size, y * size, size, size)
            }
        }

        return bi
    }



    fun getPics(): List<Picture> {
        val url = "https://bitdb.network/q/"
        val query = Base64.getEncoder().withoutPadding().encodeToString("{ request: {encoding: {b1: 'hex'},'find': {b1: '7e01'}},response: {encoding: {b2: 'hex'}}}".toByteArray())
        val headers = mapOf("key" to "qz89vqmg67zp5ek8h07llug6vcva8y903yylykv4tz")
        val response = khttp.get(
                url = url + "eyJyZXF1ZXN0Ijp7ImVuY29kaW5nIjp7ImIxIjoiaGV4In0sImZpbmQiOnsiYjEiOiI3ZTAxIn19LCJyZXNwb25zZSI6eyJlbmNvZGluZyI6eyJiMiI6ImhleCJ9fX0",
                headers = headers)


        fun convertColor(c: String) = Splitter
                .fixedLength(6)
                .split(c).map { Color.decode("#" + it) }

        val pallette = createPallettes(response.text).map { ColorObject(it.tx, it.s3, convertColor(it.b2)) }.associateBy({ it.id }, { it })

        val request = "eyJyZXF1ZXN0Ijp7ImVuY29kaW5nIjp7ImIxIjoiaGV4In0sImZpbmQiOnsiYjEiOiI3ZTAyIn19LCJyZXNwb25zZSI6eyJlbmNvZGluZyI6eyJiMyI6ImhleCIsImIyIjoiaGV4In19fQ=="

        val response2 = khttp.get(
                url = url + request,
                headers = headers)

        return createPallettes(response2.text).map { Picture(pallette.get(it.b2), it.b3, it.tx) }
    }
}