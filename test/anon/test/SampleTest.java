/*
 Copyright (c) 2000, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
  may be used to endorse or promote products derived from this software without specific
  prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package anon.test;

import junit.framework.*;
// import Testclass;

public class SampleTest extends TestCase
{
	private int m_One;
	private boolean m_bTwo;

	public SampleTest(String name)
	{
		super(name);
	}

	/**
	* Method is run before each test, for example for object creation.
	*/
	protected void setUp() {
		m_One = 0;
		m_bTwo = false;
	}

	/**
	* Method is run after each test, for example for object deletion.
	*/
   protected void tearDown() {
	   m_One = 0;
   }


	/**
	* Tests a fictive method of the Sample class with the name "doSomethingMethod".
	*/
   public void testDoSomethingMethod() {
	   for (m_One = 0; m_One < 5; m_One++); /* Simulates a method call. */
	   TestCase.assertEquals(5, m_One);

	   m_One += 18; /* Simulates a method call. */
	   TestCase.assertEquals(23, m_One);
   }


	/**
	* Tests a fictive method of the Sample class with the name "doAnything".
	*/
   public void testDoAnything() {
	   m_bTwo = true; /* Simulates a method call. */
	   TestCase.assertTrue(m_bTwo);
   }

   public void testAnException() {
	   try {
		   if (true) {
			   throw new ArrayIndexOutOfBoundsException("Sample exception");  /* Simulates a method call. */
		   }
		   TestCase.fail("Expected: ArrayIndexOutOfBoundsException");
	   } catch (ArrayIndexOutOfBoundsException e) {}

   }
}

