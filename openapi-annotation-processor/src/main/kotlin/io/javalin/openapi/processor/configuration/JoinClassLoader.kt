package io.javalin.openapi.processor.configuration

import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.ByteBuffer
import java.util.Enumeration
import java.util.Vector

/* Based on https://www.source-code.biz/snippets/java/12.htm */
class JoinClassLoader(parent: ClassLoader?, private vararg val delegateClassLoaders: ClassLoader) : ClassLoader(parent) {

    @Throws(ClassNotFoundException::class)
    override fun findClass(name: String): Class<*> {
        val path = name.replace('.', '/') + ".class"
        val url = findResource(path) ?: throw ClassNotFoundException(name)
        val byteCode: ByteBuffer =
            try {
                loadResource(url)
            } catch (e: IOException) {
                throw ClassNotFoundException(name, e)
            }
        return defineClass(name, byteCode, null)
    }

    @Throws(IOException::class)
    private fun loadResource(url: URL): ByteBuffer {
        var stream: InputStream? = null
        return try {
            stream = url.openStream()

            var initialBufferCapacity = Math.min(0x40000, stream.available() + 1)

            initialBufferCapacity =
                if (initialBufferCapacity <= 2) {
                    0x10000
                } else {
                    Math.max(initialBufferCapacity, 0x200)
                }

            var buf = ByteBuffer.allocate(initialBufferCapacity)

            while (true) {
                if (!buf.hasRemaining()) {
                    val newBuf = ByteBuffer.allocate(2 * buf.capacity())
                    buf.flip()
                    newBuf.put(buf)
                    buf = newBuf
                }

                val len = stream.read(buf.array(), buf.position(), buf.remaining())

                if (len <= 0) {
                    break
                }

                buf.position(buf.position() + len)
            }

            buf.flip()
            buf
        } finally {
            stream?.close()
        }
    }

    override fun findResource(name: String): URL? {
        for (delegate in delegateClassLoaders) {
            val resource = delegate.getResource(name)

            if (resource != null) {
                return resource
            }
        }

        return null
    }

    @Throws(IOException::class)
    override fun findResources(name: String): Enumeration<URL> {
        val vector = Vector<URL>()

        for (delegate in delegateClassLoaders) {
            val enumeration = delegate.getResources(name)

            while (enumeration.hasMoreElements()) {
                vector.add(enumeration.nextElement())
            }
        }

        return vector.elements()
    }

}