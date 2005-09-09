package jpi;

import anon.util.IXMLEncodable;

public interface PICommand
{
    /**
     * This interface reflects a simple request-reply mechanism. The next() method
	 * is called for each incoming request and returns an answer in XML format.
     *
     * @param request next incoming request
     * @return reply
     */
    abstract public PIAnswer next(PIRequest request);
}
