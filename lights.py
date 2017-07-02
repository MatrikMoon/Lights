import socket
import sys
import os
import time
import threading
		
def riceSend(data):
	try:
		sock.sendall(str(data).encode())
	except Exception as ex:
		template = "An exception of type {0} occurred. Arguments:\n{1!r}"
		message = template.format(type(ex).__name__, ex.args)
		print(message)

def moonSend(data):
	try:
		sock.sendall(str(data + "<EOF>\0").encode())
	except Exception as ex:
		template = "An exception of type {0} occurred. Arguments:\n{1!r}"
		message = template.format(type(ex).__name__, ex.args)
		print(message)

def receiver():
	while True:
		try:
			data = sock.recv(1024)
			data = data.decode().split("<EOF>")
			data = data[0]
			if len(data) > 1:
				print(data)
		except ConnectionAbortedError:
			print("Connection to zork server closed.")
			break
		except ConnectionResetError:
			print("Connection to zork server severed.")
			break
		except Exception as ex:
			template = "An exception of type {0} occurred. Arguments:\n{1!r}"
			message = template.format(type(ex).__name__, ex.args)
			print(message)

try:
	# ZorkServer connection state
	serverconnected = False
	
	# Create a TCP/IP socket
	sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

	# Connect the socket to the port where the server is listening
	#server_address = ("192.168.1.126", 9875)
	server_address = ("192.168.1.101", 10150)
	try:
		sock.connect(server_address)
		serverconnected = True
		#serverconnected = False
	except TimeoutError:
		print("Zork connection timed out")
		serverconnected = False

	if serverconnected:
		# Set up receiving
		t = threading.Thread(target=receiver)
		t.daemon = True
		t.start()
		
		moonSend(sys.argv[1].upper())

	'''
	while True:
		toSend = input()
		moonSend(toSend)
		'''
		
	# Set up inputloop
	#t2 = threading.Thread(target=inputloop)
	#t2.daemon = True
	#t2.start()
except Exception as ex:
	template = "An exception of type {0} occurred. Arguments:\n{1!r}"
	message = template.format(type(ex).__name__, ex.args)
	print(message)
finally:
	sock.close()