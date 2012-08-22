var baseUrl = 'http://davezhu.com/api/'

var seq = -1

var previousTopOfBook = null;

$(document).ready(function() {

    function formatPrice(price) {
        return price.toFixed(2);
    }

    function handleTick(tick) {
        var lastNode = $(".last");
        updatePricing(lastNode, tick.pricing);
        lastNode.effect("highlight", { color: "cyan" }, 300);
    }

    function updateTopOfBook(book) {
        updatePrevious({ bid: book.bids[0], ask: book.asks[0] });
        var bidNode = $('#top li.bid div.pricing');
        updatePricing(bidNode, book.bids[0]);
        var askNode = $('#top li.ask div.pricing')
        updatePricing(askNode, book.asks[0]);
    }

    function updatePrevious(current) {
        if (previousTopOfBook) {
            if (previousTopOfBook.bid.price != current.bid.price) {
                $('#top li.bid div.pricing').effect("highlight", { color: "red" }, 500);
                updateArrow($('#top li.bid div.label span.arrow'), previousTopOfBook.bid, current.bid);
            }
            if (previousTopOfBook.ask.price != current.ask.price) {
                $('#top li.ask div.pricing').effect("highlight", { color: "blue" }, 500);
                updateArrow($('#top li.ask div.label span.arrow'), previousTopOfBook.ask, current.ask);
            }
        }
        previousTopOfBook = current;
    }

    function updateArrow(node, previous, current) {
        if (current.price > previous.price) {
            node.removeClass('down');
            node.addClass('up');
        } else if (current.price < previous.price) {
            node.removeClass('up');
            node.addClass('down');
        } else {
            node.removeClass('up');
            node.removeClass('down');
        }
    }

    function handleBook(book) {
        updateTopOfBook(book);
        $('div.lines ul.line').each(function(i, lineNode) {
            if (i+1 < book.bids.length) {
                updatePricing($('li.bid', lineNode), book.bids[i+1]);
            }
            if (i+1 < book.asks.length) {
                updatePricing($('li.ask', lineNode), book.asks[i+1]);
            }
        });
    }

    function updatePricing(node, pricing) {
        $('.price', node).html(formatPrice(pricing.price));
        $('.qty', node).html(pricing.qty);
    }

    (function poll(url){

        $.ajax({

            url: url,

            dataType: "json",

            success: function(data){
                $.each(data, function(i, item) {
                    seq = item.seq
                    if (item.tick) {
                        // tick
                        handleTick(item);
                    } else {
                        // book data
                        handleBook(item);
                    }
                });
            },

            complete: function() {
                poll(baseUrl + '?since=' + seq)
            },

            timeout: 30000

        });

    })(baseUrl);

});
