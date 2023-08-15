from flask import Flask, request
from dotenv import load_dotenv
import os
from os.path import join, dirname
import uuid
from yookassa import Configuration, Payment

app = Flask(__name__)

payment_list = {}


def get_from_env(key):
    dotenv_path = join(dirname(__file__), '.env')
    load_dotenv(dotenv_path)
    return os.environ.get(key)


def from_yookassa(request1):
    try:
        if request1.is_json:
            if request1.json['event'] is not None:
                return True
    except KeyError:
        return False
    return False


def from_bot(request1):
    try:
        if request1.is_json:
            if request1.json['bot_event'] is not None:
                return True
    except KeyError:
        return False
    return False


def create_invoice(price, description):
    Configuration.account_id = get_from_env("SHOP_ID")
    Configuration.secret_key = get_from_env("SECRET_KEY")
    return_url = "https://t.me/UlearnProjectBot"
    payment = Payment.create({
        "amount": {
            "value": price,
            "currency": "RUB"
        },
        "confirmation": {
            "type": "redirect",
            "return_url": return_url
        },
        "capture": True,
        "description": description
    }, uuid.uuid4())
    return {'payment_id': payment.id, 'payment_url': payment.confirmation.confirmation_url}


@app.route('/', methods=['POST'])
def process():
    # requests from yookassa
    if from_yookassa(request):
        # changing payment_list data
        payment_list[request.json['object']['id']] = request.json['event']
    # requests from bot
    elif from_bot(request):
        if request.json['bot_event'] == 'get_url':
            description = request.json['description']
            price = request.json['price']
            payment = create_invoice(price, description)
            payment_list.update({payment['payment_id']: 'payment.created'})
            return {'payment_id': payment['payment_id'], 'payment_url': payment['payment_url']}
        elif request.json['bot_event'] == 'check_id':
            return {'checking_result': payment_list.get(request.json['id'])}
    print(payment_list)
    return {'event': 'incorrect_request'}


if __name__ == '__main__':
    app.run()
