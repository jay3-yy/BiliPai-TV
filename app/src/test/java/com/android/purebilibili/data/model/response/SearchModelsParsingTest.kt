package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchModelsParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun decodeSearchTypeResponse_acceptsStringAndNumericFields() {
        val payload = """
            {
              "code": 0,
              "message": "",
              "data": {
                "page": 1,
                "numPages": 3,
                "numResults": 60,
                "result": [
                  {
                    "id": "12345",
                    "bvid": "BV1xx411c7mD",
                    "title": "test",
                    "pic": "//i0.hdslb.com/test.jpg",
                    "author": "tester",
                    "play": "5678",
                    "video_review": 99,
                    "duration": 180,
                    "pubdate": "1730000000",
                    "mid": "778899"
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<SearchTypeResponse>(payload)
        val item = response.data?.result?.first()

        assertEquals(12345L, item?.id)
        assertEquals(5678, item?.play)
        assertEquals(99, item?.video_review)
        assertEquals("180", item?.duration)
        assertEquals(1730000000L, item?.pubdate)
        assertEquals(778899L, item?.mid)
    }

    @Test
    fun decodeSearchTypeResponse_acceptsChineseUnitCount() {
        val payload = """
            {
              "code": 0,
              "message": "",
              "data": {
                "page": 1,
                "numPages": 1,
                "numResults": 1,
                "result": [
                  {
                    "id": 1,
                    "bvid": "BV1",
                    "title": "unit",
                    "pic": "",
                    "author": "",
                    "play": "1.2万",
                    "video_review": "3.4万",
                    "duration": "03:21",
                    "pubdate": 0,
                    "mid": 0
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<SearchTypeResponse>(payload)
        val item = response.data?.result?.first()

        assertEquals(12000, item?.play)
        assertEquals(34000, item?.video_review)
        assertEquals("03:21", item?.duration)
    }
}
