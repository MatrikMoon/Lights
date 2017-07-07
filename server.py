import time
import RPi.GPIO as GPIO
import sys
import socket
import threading
import os

status = "OFF"
clients = []

#Set the status and update all connected clients
def setStatus(newStatus):
    global status
    if status != newStatus:
        if newStatus == "ON":
            GPIO.output(16, GPIO.HIGH)
        elif newStatus == "OFF":
            GPIO.output(16, GPIO.LOW)
        else:
            return
        for client in clients:
            send(newStatus, client)
        status = newStatus

#Parse commands received from clients
def parseCommands(data, client):
    global status
    #print(clients)
    if data == "ON":
        setStatus("ON")
    elif data == "OFF":
        setStatus("OFF")
    elif data == "REQUEST_STATUS":
        send(status, client)
    elif data == "SHUTDOWN":
        return "SHUTDOWN"
    elif data == "REBOOT":
        return "REBOOT"
    else:
        print(data)
        clients.remove(client)
        return "CLOSE"

#Send data to specified client
def send(data, client):
    global clients
    try:
        client.sendall(str(data + "<EOF>\0").encode())
    except Exception as ex:
        #print(str(ex))
        #print(str("Removing client"))
        clients.remove(client)

#Receive data from specified client
def receiver(client):
    while True:
        try:
            data = client.recv(1024)
            data = data.decode().split("<EOF>")
            data = data[0]
            ret = "CLOSE"
            if len(data) > 1:
                ret = parseCommands(data, client)
            if ret == "CLOSE":
                client.close()
                return
            elif ret == "SHUTDOWN":
                client.close()
                sock.close()
                return
            elif ret == "REBOOT":
                client.close()
                sock.close()
                os.system("reboot")
                return
        except Exception as ex:
                template = "An exception of type {0} occurred. Arguments:\n{1!r}"
                messge = template.format(type(ex).__name__, ex.args)

#Set up socket server
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_address = ("192.168.1.126", 9875)
sock.bind(server_address)
sock.listen(5) #5 is an arbitrary value, according to docs it stands for "Number of unauthorized connections before the server stops listening"

#Set up GPIO
GPIO.setmode(GPIO.BCM)
GPIO.setup(16, GPIO.OUT)

#Infinitely accept connections
while True:
    client, addr = sock.accept()
    clients.append(client)
    t = threading.Thread(target=receiver, args=(client,))
    t.daemon = True
    t.start()

#Will never reach here, but if it did it would clean up the GPIO
GPIO.cleanup()