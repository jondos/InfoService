public final class JAP 
	{

    public static void main(String[] argv)
			{
				String vers = System.getProperty("java.version");
				if (vers.compareTo("1.1.2") < 0)
					{
							System.out.println("!!!WARNING: JAP must be run with a " +
			       "										1.1.2 or higher version VM!!!");
					}

				JAPModel model = new JAPModel();
				JAPSplash splash = new JAPSplash(model);
				splash.show(); // show splash screen as soon as possible
				model.load();
				model.addJAPObserver(model);


				JAPView view = new JAPView (model, model.TITLE);
				model.addJAPObserver(view);
				splash.dispose();
				view.show();

				model.setAnonMode(true);
    }
}
