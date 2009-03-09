package misc;

import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.InputStream;

import anon.util.Util;

public class GenerateUpload
	{
		private static final int MAX_WAIT_BETWEEN_CHANNELS = 1000;
		private static final int MAX_CHANNELS = 1000	;
		private static final int MAX_SINGLE_CALL_UPLOAD = 18000;
		public int MAX_WAIT_BETWEEN_SINGLE_CALL_UPLOAD=10;
	static int MAX_UPLOAD = 270000;
		private static byte[] randomData;
		static Random rand;
		static int success = 0;
		static int failed = 0;

		static synchronized void incFailed()
			{
				failed++;
			}

		static synchronized void incSuccess()
			{
				success++;
			}

		public static void main(String[] args) throws UnknownHostException,
				IOException
			{
				rand = new Random();

				randomData = new byte[500000];
				for (int i = 0; i < randomData.length; i++)
					randomData[i] = (byte) i;
				GenerateUpload g = new GenerateUpload();
				g.start();
			}

	
		void start()
			{
				Upload uploads[] = new Upload[MAX_CHANNELS];
				for (int i = 0; i < MAX_CHANNELS; i++)
					{
						uploads[i] = new Upload(rand.nextInt(MAX_UPLOAD), i);
						try
							{
								Thread.sleep(rand.nextInt(MAX_WAIT_BETWEEN_CHANNELS));
							}
						catch (InterruptedException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
					}
				for (int i = 0; i < MAX_CHANNELS; i++)
					{
						uploads[i].join();
					}
				System.out.println(success + " ok / " + failed + " failed");
			}

		class Upload implements Runnable
			{
				int maxLen;
				int id;
				Thread t;

				public Upload(int maxL, int ID)
					{
						id = ID;
						maxLen = maxL;
						t = new Thread(this);
						t.start();
					}

				public void join()
					{
						try
							{
								t.join();
							}
						catch (InterruptedException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
					}

				public void run()
					{
						int l = 0;
						Socket s = null;
						try
							{
								s = new Socket("127.0.0.1", 7777);
							}
						catch (Exception e1)
							{
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						try
							{
								// rand.nextBytes(randomData);
								OutputStream out = s.getOutputStream();
								Download d = new Download(s.getInputStream(), maxLen, id);
								out.write("CONNECT 127.0.0.1:7 HTTP/1.0\n\r\n\r".getBytes()); // Note:
								// Connects
								// to
								// the
								// ECHO
								// service
								// through
								// the
								// cascade
								// /
								// proxy
								int currentPos = 0;
								int len = 0;
								while (l < maxLen)
									{
										len = Math.min(rand.nextInt(MAX_SINGLE_CALL_UPLOAD),
												randomData.length - currentPos);
										len = Math.min(len, maxLen - l);
										out.write(randomData, currentPos, len);
										currentPos += len;
										currentPos %= randomData.length;
										l += len;
										if ((l % 1000000) == 0) System.out.println("Sent: " + l
												/ 1000000 + " MBytes");
										try
											{
												Thread.sleep(rand.nextInt(MAX_WAIT_BETWEEN_SINGLE_CALL_UPLOAD));
											}
										catch (InterruptedException e)
											{
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
									}
								System.out.println("Upload: " + id + " Sent: " + l + " Bytes");
								d.join();
								s.close();
							}
						catch (Throwable t)
							{
								System.out.println("Exception in Upload -- l=" + l);
								t.printStackTrace();
							}
					}

				class Download implements Runnable
					{
						InputStream in;
						int maxLen;
						int id;
						Thread t;

						Download(InputStream i, int maxL, int ID)
							{
								id = ID;
								in = i;
								maxLen = maxL;
								t = new Thread(this);
								t.start();
							}

						public void join()
							{
								try
									{
										t.join();
									}
								catch (InterruptedException e)
									{
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
							}

						public void run()
							{
								try
									{
										// LineNumberReader inr=new LineNumberReader(new
										// InputStreamReader(in));
										// System.out.println(inr.readLine());
										// String s=inr.readLine();
										// s=inr.readLine();

										byte[] buff = new byte[10000];
										int l = 0;
										long reported = 1000000;
										int currentPos = 0;
										int h = in.read(buff, 0, 39);// skip proxy response headers
										h = in.read(buff, 0, 1); // Strange thing here: we got
										// (sometimes?) and extra chr(13)
										// after the HTTP response headers
										// - why ???
										while (l < maxLen)
											{
												int r = Math.min(buff.length, randomData.length
														- currentPos);
												r = Math.min(r, maxLen - l);

												r = in.read(buff, 0, r);
												for (int z = 0; z < r; z++)
													{
														if (buff[z] != randomData[currentPos+z])
															{
																System.out.println("Download: " + id
																		+ " -- Read error! -- Position: " + l
																		+ " -- Read: " + (buff[z]&0x00FF) + " -- Expected: "
																		+ (randomData[currentPos+z]&0x00FF));
																incFailed();
																return;
															}
													}
												l += r;
												currentPos += r;
												currentPos %= randomData.length;
												if (l >= reported)
													{
														System.out.println("Read: " + l / 1000000.0
																+ " MBytes");
														reported += 1000000;
													}
											}
										System.out.println("Download: " + id
												+ " -- Succesfully received: " + l + " Bytes");
										incSuccess();
									}

								catch (Exception e)
									{
										System.out.println("Read failed!");
									}
							}
					}
			}
	}
