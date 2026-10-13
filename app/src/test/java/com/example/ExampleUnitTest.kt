package com.example

import com.example.data.local.CourseEntity
import com.example.data.local.LectureEntity
import com.example.data.network.KoyebApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testKoyebApiClientUrlResolution() {
        val client = KoyebApiClient.instance
        assertEquals("https://sick-nikaniki-shubham8208989-6d7c863a.koyeb.app", client.baseUrl)

        val streamUrl = client.buildStreamUrl("token123")
        assertEquals("https://sick-nikaniki-shubham8208989-6d7c863a.koyeb.app/watch-stream/token123", streamUrl)

        val fullUrl = client.buildStreamUrl("https://custom.cdn/video.mp4")
        assertEquals("https://custom.cdn/video.mp4", fullUrl)

        val pdfUrl = client.buildPdfUrl("pdftoken456")
        assertEquals("https://sick-nikaniki-shubham8208989-6d7c863a.koyeb.app/pdf-view/pdftoken456", pdfUrl)
    }

    @Test
    fun testCourseAndLectureEntities() {
        val course = CourseEntity(
            id = "c1",
            title = "Golang Backend Mastery",
            category = "Backend",
            description = "Complete guide to TG-coursestreamBot backend",
            instructor = "Vortex"
        )
        assertNotNull(course)
        assertEquals("c1", course.id)

        val lecture = LectureEntity(
            id = "lec1",
            courseId = "c1",
            title = "Introduction to Stream Pipes",
            durationSeconds = 1200,
            fileType = "VIDEO"
        )
        assertEquals(1200, lecture.durationSeconds)
        assertTrue(lecture.fileType == "VIDEO")
    }
}
