package com.example.common.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The type Resource reader utils.
 * To read the resource as String
 * @author bnasslahsen
 */
public final class ResourceReaderUtils {

	private ResourceReaderUtils() {}

	/**
	 * Read file to string .
	 *
	 * @param path the path
	 * @return the string
	 */
	public static String readFileToString(String path) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Resource resource = resourceLoader.getResource(path);
		return asString(resource);
	}

	private static String asString(Resource resource) {
		try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
			return FileCopyUtils.copyToString(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
