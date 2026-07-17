package com.demo;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple servlet compiled with custom compilerArgs ('-parameters', '-Xlint:-processing').
 * Verifies that compilerArgs are read by getGradleCompilerOptions() and do not break
 * hot-reload compilation in dev mode.
 */
@WebServlet(urlPatterns = "/hello")
public class HelloServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.getWriter().append("hello world");
    }
}
