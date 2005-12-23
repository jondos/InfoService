/*
 Copyright (c) 2000 - 2005, The JAP-Team
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
package gui;

import java.util.Locale;
import java.util.Vector;

import anon.util.Util;

/**
 * Objects of this class store a country code an can translate it into
 * the localised name of the corresponding country.
 *
 * @author Kuno G. Gruen
 * @author Rolf Wendolsky
 * @see http://www.iso.ch/iso/en/prods-services/iso3166ma/02iso-3166-code-lists/iso_3166-1_decoding_table.html
 * @version ISO 3166 on 25.10.05
 */
public class CountryMapper
{
	private static final String[] ms_ctrArr =
		{
		// Officially assigned code elements
		"AD", "AE", "AF", "AG", "AI", "AL", "AM", "AN", "AO", "AQ", "AR", "AS",
		"AT", "AU", "AW", "AX", "AZ", "BA", "BB", "BD", "BE", "BF", "BG", "BH",
		"BI", "BJ", "BM", "BN", "BO", "BR", "BS", "BT", "BV", "BW", "BY", "BZ",
		"CA", "CC", "CD", "CF", "CG", "CH", "CI", "CK", "CL", "CM", "CN", "CO",
		"CR", "CS", "CU", "CV", "CX", "CY", "CZ", "DE", "DJ", "DK", "DM", "DO",
		"DZ", "EC", "EE", "EG", "EH", "ER", "ES", "ET", "FI", "FJ", "FK", "GA",
		"GB", "FM", "FO", "GP", "GQ", "GR", "GS", "GT", "GU", "GW", "GY", "GD",
		"GE", "GF", "GH", "GI", "GL", "GM", "GN", "HM", "HN", "HR", "HT", "HU",
		"IQ", "IR", "IS", "IT", "LR", "LS", "LT", "LU", "LV", "HK", "ID", "IE",
		"IL", "IN", "IO", "KE", "KG", "KH", "KI", "JM", "JO", "JP", "KM", "KN",
		"KP", "KR", "KW", "KY", "KZ", "LA", "LB", "LC", "LI", "LK", "LY", "MA",
		"MC", "MD", "MG", "MH", "MK", "ML", "MM", "MN", "MO", "MP", "MQ", "MR",
		"MS", "MT", "MU", "MV", "MW", "MX", "MY", "MZ", "NA", "NC", "NE", "NF",
		"NG", "NI", "NL", "NO", "NP", "NR", "NU", "NZ", "PA", "PE", "PF", "PG",
		"PH", "OM", "PK", "PL", "PM", "PN", "RU", "RW", "SV", "PR", "PS", "PT",
		"QA", "RE", "PW", "PY", "SY", "SZ", "SA", "SB", "SC", "SD", "SE", "SG",
		"SH", "SI", "SJ", "SK", "SL", "SM", "SN", "SO", "RO", "SR", "ST", "TC",
		"TD", "TF", "TG", "TH", "TJ", "TK", "TL", "TM", "TN", "TO", "TR", "TT",
		"TV", "TW", "TZ", "UG", "UA", "UY", "UZ", "UM", "US", "VI", "VN", "ZM",
		"YT", "VU", "VA", "VC", "VE", "VG", "WF", "WS", "ZA", "YE", "ZW",
		/*
		 // Exceptionally reserved code elements
		   "AC", "DG", "EA", "CP", "EU", "FX", "GG", "IC", "JE", "IM", "UK",

		 // User-assigned code elements
		   "TA", "QM", "QN", "QO", "QP", "QQ", "QR", "QS", "QT", "QU", "QV", "QW",
		   "QX", "QY", "QZ", "XA", "XB", "XC", "XD", "XE", "XF", "XG", "XH", "XI",
		   "XJ", "XK", "XL", "XM", "XN", "XO", "XP", "XQ", "XR", "XS", "XT", "XU",
		   "XV", "XW", "XX", "XY", "XZ", "ZZ"

		 */
	};

	private static final String MSG_CHOOSE_COUNTRY = CountryMapper.class.getName() + "_ChooseCountry";

	private final int MAX_LENGTH;
	private boolean m_bUseDefaultLocale;
	private String m_iso2;
	private Locale m_locale;


	/**
	 * Constructs an empty CountryMapper object. Its toString() method
	 * returns a message that requests to choose a valid country code.
	 * The message is defined by the default Locale.
	 */
	public CountryMapper()
	{
		this(null, 0);
	}

	/**
	 * Constructs an empty CountryMapper object. Its toString() method
	 * returns a message that requests to choose a valid country code.
	 * The message is defined by the default Locale.
	 * @param a_maxCountryLength the maximum length of the toString() output
	 */
	public CountryMapper(int a_maxCountryLength)
	{
		this(null, a_maxCountryLength);
	}

	/**
	 * Constructs a new CountryMapper that uses the default Locale to
	 * translate its country code.
	 * @see getLocalisedCountries(int)
	 * @param a_ISO2CountryCode a two-letter country code
	 * @param a_maxCountryLength the maximum length of the toString() output
	 * @throws IllegalArgumentException
	 */
	public CountryMapper(String a_ISO2CountryCode, int a_maxCountryLength)
		throws IllegalArgumentException
	{
		this(a_ISO2CountryCode, a_maxCountryLength, null);
	}

	/**
	 * Constructs a new CountryMapper that uses the default Locale to
	 * translate its country code.
	 * @see getLocalisedCountries(int)
	 * @param a_ISO2CountryCode a two-letter country code
	 * @throws IllegalArgumentException
	 */
	public CountryMapper(String a_ISO2CountryCode)
		throws IllegalArgumentException
	{
		this(a_ISO2CountryCode, 0, null);
	}

	/**
	 * Constructs a new CountryMapper that uses a specific Locale to
	 * translate its country code.
	 * @see getLocalisedCountries(int, Locale)
	 * @param a_ISO2CountryCode a two-letter country code
	 * @param a_locale a Locale
	 * @throws IllegalArgumentException
	 */
	public CountryMapper(String a_ISO2CountryCode, Locale a_locale)
		throws IllegalArgumentException
	{
		this(a_ISO2CountryCode, 0, a_locale);
	}

	/**
	 * Constructs a new CountryMapper that uses a specific Locale to
	 * translate its country code.
	 * @see getLocalisedCountries(int, Locale)
	 * @param a_ISO2CountryCode a two-letter country code
	 * @param a_locale a Locale
	 * @param a_maxCountryLength the maximum length of the toString() output
	 * @throws IllegalArgumentException
	 */
	public CountryMapper(String a_ISO2CountryCode, int a_maxCountryLength,
						 Locale a_locale) throws IllegalArgumentException
	{
		MAX_LENGTH = a_maxCountryLength;

		if (a_ISO2CountryCode == null || a_ISO2CountryCode.trim().length() == 0)
		{
			a_ISO2CountryCode = "";
		}
		if (a_ISO2CountryCode.length() > 0 && a_ISO2CountryCode.length() != 2)
		{
			throw new IllegalArgumentException(
				"ISO Country code must have a length of two characters!");
		}

		m_iso2 = a_ISO2CountryCode.trim().toUpperCase();

		if (a_locale == null)
		{
			m_bUseDefaultLocale = true;
			m_locale = Locale.getDefault();
		}
		else
		{
			m_bUseDefaultLocale = false;
			m_locale = a_locale;
		}
	}

	/**
	 * Returns a Vector with CountryMappers for all officially assigned
	 * country codes. The constructed CountryMappers use the default locale.
	 * @return a Vector with CountryMappers for all officially assigned
	 * country codes
	 */
	public static Vector getLocalisedCountries()
	{
		return (getLocalisedCountries(0, null));
	}

	/**
	 * Returns a Vector with CountryMappers for all officially assigned
	 * country codes. The constructed CountryMappers use the default locale.
	 * @param a_loc the locale that is used in all contructed CountryMappers
	 * @return a Vector with CountryMappers for all officially assigned
	 * country codes
	 */
	public static Vector getLocalisedCountries(Locale a_loc)
	{
		return (getLocalisedCountries(0, a_loc));
	}

	/**
	 * Returns a Vector with CountryMappers for all officially assigned
	 * country codes. The constructed CountryMappers use the default locale.
	 * @param a_maxCountryLength the maximum length of the toString() output
	 * @return a Vector with CountryMappers for all officially assigned
	 * country codes
	 */
	public static Vector getLocalisedCountries(int a_maxCountryLength)
	{
		return (getLocalisedCountries(a_maxCountryLength, null));
	}

	/**
	 * Returns a Vector with CountryMappers for all officially assigned
	 * country codes.
	 * @param a_loc the locale that is used in all contructed CountryMappers
	 * @param a_maxCountryLength the maximum length of the toString() output
	 * @return a Vector with CountryMappers for all officially assigned
	 * country codes
	 */
	public static Vector getLocalisedCountries(int a_maxCountryLength,
											   Locale a_loc)
	{
		Vector localisedCountries = new Vector();

		for (int i = 0; i < ms_ctrArr.length; i++)
		{
			localisedCountries.addElement(
				 new CountryMapper(ms_ctrArr[i], a_maxCountryLength, a_loc));
		}
		return Util.sortStrings(localisedCountries);
	}

	/**
	 * Returns the ISO country code stored in this CountryMapper object. The country code may be an empty
	 * String of the length zero or a valid two-letter country code.
	 * @return the ISO country code stored in this CountryMapper object
	 */
	public String getISOCountryCode()
	{
		return m_iso2;
	}

	/**
	 * Returns if the ISO country codes of two CountryMapper objects are equal.
	 * @param a_object an Object
	 * @return true if the ISO country codes of two CountryMapper objects are
	 *         equal; false otherwise
	 */
	public boolean equals(Object a_object)
	{
		if (a_object == null || !(a_object instanceof CountryMapper))
		{
			return false;
		}
		return getISOCountryCode().equals(
			  ((CountryMapper)a_object).getISOCountryCode());
	}

	/**
	 * Returns the hash code of the ISO country code.
	 * @return the hash code of the ISO country code
	 */
	public int hashCode()
	{
		return getISOCountryCode().hashCode();
	}

	/**
	 * Returns the localised name of the ISO country code of this
	 * CountryMapper object. The output may depend on the current locale or
	 * on the locale that may be defined in the constructor.
	 * @return the localised name of the ISO country code
	 */
	public String toString()
	{
		String strCName;
		String temp;
		Locale locale;

		if (m_iso2.length() == 0)
		{
			strCName = JAPMessages.getString(MSG_CHOOSE_COUNTRY);
		}
		else
		{
			if (m_bUseDefaultLocale)
			{
				locale = Locale.getDefault();
			}
			else
			{
				locale = m_locale;
			}

			strCName = getJRETransaltionOfCountryCode(m_iso2, locale);

			if (strCName == null || strCName.equals(m_iso2) ||
				// some old JREs cannot resolve the country code correctly
				strCName.equals(getJRETransaltionOfCountryCode("AA", locale)))
			{
				temp = CountryMapper.class.getName() + "_" + m_iso2;
				strCName = JAPMessages.getString(temp);
				if (strCName.equals(temp))
				{
					// the country could not be translated
					strCName = m_iso2;
				}
			}
		}

		if (MAX_LENGTH > 0 && strCName.length() > MAX_LENGTH)
		{
			strCName = strCName.substring(0, MAX_LENGTH);
		}

		return strCName;
	}

	private static String getJRETransaltionOfCountryCode(String a_contryCode, Locale a_locale)
	{
		return new Locale(a_locale.getLanguage(), a_contryCode).getDisplayCountry();
	}
}
