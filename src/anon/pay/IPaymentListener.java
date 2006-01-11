package anon.pay;

import java.util.EventListener;

import anon.pay.xml.XMLErrorMessage;
import anon.util.captcha.IImageEncodedCaptcha;

/**
 * GUI classes can implement this interface and register with the Payment library
 * to be notified about payment specific events
 * @version 1.0
 * @author Bastian Voigt, Tobias Bayer
 */
public interface IPaymentListener extends EventListener
{
	/**
	 * The AI has signaled that the current cascade has to be payed for.
	 * @param acc PayAccount
	 */
	void accountCertRequested(boolean usingCurrentAccount);

	/**
	 * The AI has signaled an error.
	 * @param acc PayAccount
	 */
	void accountError(XMLErrorMessage msg);

	/**
	 * The active account changed.
	 * @param acc PayAccount the account which is becoming active
	 */
	void accountActivated(PayAccount acc);

	/**
	 * An account was removed
	 * @param acc PayAccount the account which was removed
	 */
	void accountRemoved(PayAccount acc);

	/**
	 * An account was added
	 * @param acc PayAccount the new Account
	 */
	void accountAdded(PayAccount acc);

	/**
	 * The credit changed for the given account.
	 * @param acc PayAccount
	 */
	void creditChanged(PayAccount acc);

	/**
	 * Captcha retrieved
	 */
	void gotCaptcha(Object a_source, IImageEncodedCaptcha a_captcha);
	}
