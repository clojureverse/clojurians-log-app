#!/usr/bin/env ruby

require 'time'
require 'net/http'
require 'json'

ENDPOINT = 'https://metrics-api.librato.com/v1/metrics'

def read_log(path)
  IO.read(path).split("\n").map { |l| d,*r = l.split(" - "); [Time.parse(d), *r] rescue nil }.compact
end

def log_since(log, time)
  log.select { |l| l[0] >= time }
end

def histo(values)
  values.each.with_object(Hash.new(0)) do |v, h|
    h[v]+=1
  end
end

def POST(uri, headers = {})
  uri = URI(uri)
  http = Net::HTTP.new(uri.host, uri.port)
  http.use_ssl = true if uri.scheme == 'https'
  request = Net::HTTP::Post.new(uri.request_uri, headers)
  yield request
  response = http.request(request)
end

def req_time_bucket(t)
  if t > 10
    "10s+"
  else
    "#{t.round}s"
  end
end

loop do
  log = read_log('/var/log/nginx/access.log')
  log = log_since(log, Time.now - 60)

  unless log.empty?
    codes = histo(log.map{|l| l[2]})
    request_times = histo(log.map{|l| req_time_bucket(l[3].to_f)})

    codes = codes.map { |k,v| ["HTTP_#{k}", v] }.to_h
    request_times = request_times.map { |k,v| ["req_time_#{k}", v]}.to_h

    payload = codes.merge(request_times).map.with_index do |(name,count), idx|
      {"gauges[#{idx}][name]" => name,
       "gauges[#{idx}][value]" => count,
       "gauges[#{idx}][source]" => 'clojurians-log'
      }
    end.reduce(&:merge)

    p payload

    res = POST(ENDPOINT, {'Content-Type' => 'application/x-www-form-urlencoded'}) do |req|
      req.basic_auth ENV['LIBRATO_EMAIL'], ENV['LIBRATO_TOKEN']
      req.set_form_data(payload)
    end

    p res
    p res.body
  end

  sleep 60
end
